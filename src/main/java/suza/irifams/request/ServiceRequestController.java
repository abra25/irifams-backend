package suza.irifams.request;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.enums.RequestStatus;
import suza.irifams.enums.Role;
import suza.irifams.notification.Notification;
import suza.irifams.notification.NotificationRepository;
import suza.irifams.notification.NotificationService;
import suza.irifams.plot.Plot;
import suza.irifams.plot.PlotRepository;
import suza.irifams.user.User;
import org.springframework.security.core.Authentication;
import suza.irifams.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestRepository repository;
    private final UserRepository userRepository;
    private final PlotRepository plotRepository;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;

    /*
     * Farmer submits request
     */

    @PreAuthorize("hasRole('FARMER')")
    @PostMapping
    public ResponseEntity<?> submitRequest(

            @RequestParam Long farmerId,
            @RequestParam Long plotId,
            @RequestBody ServiceRequest request

    ) {

        User farmer = userRepository
                .findById(farmerId)
                .orElseThrow();

        Plot plot = plotRepository
                .findById(plotId)
                .orElseThrow();

        request.setFarmer(farmer);
        request.setPlot(plot);

        request.setStatus(RequestStatus.PENDING);
        request.setPaymentConfirmed(false);
        request.setCreatedAt(LocalDateTime.now());

        // Save request once only
        ServiceRequest savedRequest =
                repository.save(request);

        // Create notification
        // Farmer
        notificationService.notify(
                farmer,
                "Your request has been submitted successfully."
        );

// Supervisor wa block
        userRepository.findByRoleAndBlockName(
                Role.SUPERVISOR,
                plot.getBlock()
        ).forEach(supervisor ->

                notificationService.notify(
                        supervisor,
                        "New service request has been submitted for Plot "
                                + plot.getPlotNo()
                )

        );

// All Admins
        userRepository.findByRole(Role.ADMIN)
                .forEach(admin ->

                        notificationService.notify(
                                admin,
                                "New service request submitted by "
                                        + farmer.getFullName()
                        )

                );

        // Audit log
        auditLogger.log(
                farmer.getUsername(),
                "REQUEST SUBMITTED",
                "REQUEST",
                "Submitted "
                        + request.getServiceType(),
                "SUCCESS"
        );

        return ResponseEntity.ok(savedRequest);
    }

    /*
     * Get all requests
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @GetMapping
    public List<ServiceRequest> getAllRequests() {

        return repository.findAll();

    }

    /*
     * Farmer requests
     */

    @PreAuthorize(
            "hasAnyRole('FARMER','ADMIN','SUPERVISOR')")
    @GetMapping("/farmer/{farmerId}")
    public List<ServiceRequest> getFarmerRequests(
            @PathVariable Long farmerId
    ) {

        return repository.findByFarmerId(farmerId);

    }

    /*
     * Approve Request
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long id,
            @RequestBody ApproveRequest dto,
            Authentication authentication
    ) {

        return repository.findById(id)

                .map(req -> {

                    if(authentication.getAuthorities()
                            .stream()
                            .anyMatch(a ->
                                    a.getAuthority()
                                            .equals("ROLE_SUPERVISOR"))){

                        User supervisor =
                                userRepository
                                        .findByUsername(
                                                authentication.getName()
                                        )
                                        .orElseThrow();

                        if(!req.getPlot()
                                .getBlock()
                                .equals(
                                        supervisor.getBlockName())){

                            auditLogger.log(
                                    supervisor.getUsername(),
                                    "REQUEST APPROVED",
                                    "REQUEST",
                                    "Unauthorized approval attempt",
                                    "FAILED"
                            );

                            return ResponseEntity
                                    .status(403)
                                    .body("Access denied");
                        }
                    }

                    req.setStatus(
                            RequestStatus.WAITING_PAYMENT
                    );

                    req.setAmount(dto.getAmount());

                    req.setControlNumber(
                            "CN-" + System.currentTimeMillis()
                    );

                    repository.save(req);

                    repository.save(req);

                    auditLogger.log(
                            req.getFarmer()
                                    .getUsername(),

                            "REQUEST APPROVED",

                            "REQUEST",

                            "Request ID "
                                    + req.getId()
                                    + " approved",

                            "SUCCESS"
                    );

                    // Farmer
                    notificationService.notify(

                            req.getFarmer(),

                            "Your request #" + req.getId()
                                    + " has been approved."

                    );

// Supervisor aliyefanya approve
                    User supervisor = userRepository
                            .findByUsername(authentication.getName())
                            .orElseThrow();

                    notificationService.notify(

                            supervisor,

                            "You approved Request #" + req.getId()

                    );

// Admins
                    userRepository.findByRole(Role.ADMIN)
                            .forEach(admin ->

                                    notificationService.notify(

                                            admin,

                                            "Request #" + req.getId()
                                                    + " approved."

                                    )

                            );

                    return ResponseEntity.ok(req);

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            "UNKNOWN",
                            "REQUEST APPROVED",
                            "REQUEST",
                            "Failed to approve request",
                            "FAILED"
                    );

                    return ResponseEntity.notFound().build();

                });
    }
    /*
     * Reject Request
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long id
    ) {

        return repository.findById(id)

                .map(req -> {

                    req.setStatus(
                            RequestStatus.REJECTED
                    );

                    repository.save(req);

                    auditLogger.log(
                            req.getFarmer().getUsername(),
                            "REQUEST REJECTED",
                            "REQUEST",
                            "Request ID "
                                    + req.getId()
                                    + " rejected",
                            "SUCCESS"
                    );

                    notificationService.notify(

                            req.getFarmer(),

                            "Your request #"
                                    + req.getId()
                                    + " has been rejected."

                    );

                    userRepository.findByRole(Role.ADMIN)
                            .forEach(admin ->

                                    notificationService.notify(

                                            admin,

                                            "Request #" + req.getId()
                                                    + " has been rejected."

                                    )

                            );
                    return ResponseEntity.ok(req);

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            "UNKNOWN",
                            "REQUEST REJECTED",
                            "REQUEST",
                            "Failed to reject request",
                            "FAILED"
                    );

                    return ResponseEntity.notFound().build();

                });

    }

    /*
     * Generate Control Number
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/generate-control")
    public ResponseEntity<?> generateControlNumber(
            @PathVariable Long id
    ) {

        return repository.findById(id)

                .map(req -> {

                    req.setControlNumber(
                            "CN-" +
                                    System.currentTimeMillis()
                    );

                    req.setStatus(
                            RequestStatus.WAITING_PAYMENT
                    );

                    repository.save(req);

                    auditLogger.log(
                            req.getFarmer().getUsername(),
                            "CONTROL NUMBER GENERATED",
                            "PAYMENT",
                            "Control number generated for Request "
                                    + req.getId(),
                            "SUCCESS"
                    );

                    notificationService.notify(

                            req.getFarmer(),

                            "Control Number generated: "
                                    + req.getControlNumber()

                    );

                    userRepository.findByRole(Role.ADMIN)
                            .forEach(admin ->

                                    notificationService.notify(

                                            admin,

                                            "Control Number generated for Request #"
                                                    + req.getId()

                                    )

                            );
                    return ResponseEntity.ok(req);

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            "UNKNOWN",
                            "CONTROL NUMBER GENERATED",
                            "PAYMENT",
                            "Failed to generate control number",
                            "FAILED"
                    );

                    return ResponseEntity.notFound().build();

                });
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/my-requests")
    public List<ServiceRequest> getMyRequests(
            Authentication authentication
    ){

        String username =
                authentication.getName();

        User supervisor =
                userRepository
                        .findByUsername(username)
                        .orElseThrow();

        return repository
                .findByPlotBlockOrderByCreatedAtDesc(
                        supervisor.getBlockName()
                );
    }

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/my-farm-requests")
    public List<ServiceRequest> getMyFarmRequests(
            Authentication authentication
    ){

        String username =
                authentication.getName();

        User farmer =
                userRepository
                        .findByUsername(username)
                        .orElseThrow();

        return repository
                .findByFarmerId(
                        farmer.getId()
                );
    }

}