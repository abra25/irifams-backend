package suza.irifams.request;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.enums.RequestStatus;
import suza.irifams.notification.Notification;
import suza.irifams.notification.NotificationRepository;
import suza.irifams.plot.Plot;
import suza.irifams.plot.PlotRepository;
import suza.irifams.user.User;
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
    private final NotificationRepository notificationRepository;
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
        notificationRepository.save(

                Notification.builder()

                        .message(
                                "Your request has been submitted successfully."
                        )

                        .user(farmer)

                        .isRead(false)

                        .createdAt(LocalDateTime.now())

                        .build()

        );

        // Audit log
        auditLogger.log(
                farmer.getUsername(),
                "SUBMIT",
                "REQUEST",
                "Submitted " + request.getServiceType()
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
            @PathVariable Long id
    ) {

        return repository.findById(id)

                .map(req -> {

                    req.setStatus(
                            RequestStatus.APPROVED
                    );

                    repository.save(req);

                    auditLogger.log(
                            req.getFarmer().getUsername(),
                            "APPROVE",
                            "REQUEST",
                            "Request ID "
                                    + req.getId()
                                    + " approved"
                    );

                    notificationRepository.save(

                            Notification.builder()

                                    .message(
                                            "Your request #" +
                                                    req.getId() +
                                                    " has been approved."
                                    )

                                    .user(req.getFarmer())

                                    .isRead(false)

                                    .createdAt(LocalDateTime.now())

                                    .build()

                    );

                    return ResponseEntity.ok(req);

                })

                .orElse(
                        ResponseEntity.notFound().build()
                );

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
                            "REJECT",
                            "REQUEST",
                            "Request ID "
                                    + req.getId()
                                    + " rejected"
                    );

                    notificationRepository.save(

                            Notification.builder()

                                    .message(
                                            "Your request #" +
                                                    req.getId() +
                                                    " has been rejected."
                                    )

                                    .user(req.getFarmer())

                                    .isRead(false)

                                    .createdAt(LocalDateTime.now())

                                    .build()

                    );

                    return ResponseEntity.ok(req);

                })

                .orElse(
                        ResponseEntity.notFound().build()
                );

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
                            "CONTROL_NUMBER",
                            "PAYMENT",
                            "Control number generated for Request "
                                    + req.getId()
                    );

                    notificationRepository.save(

                            Notification.builder()

                                    .message(
                                            "Control Number generated: "
                                                    + req.getControlNumber()
                                    )

                                    .user(req.getFarmer())

                                    .isRead(false)

                                    .createdAt(LocalDateTime.now())

                                    .build()

                    );

                    return ResponseEntity.ok(req);

                })

                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

}