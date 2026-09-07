package suza.irifams.request;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.email.EmailService;
import suza.irifams.enums.RequestStatus;
import suza.irifams.enums.Role;
import suza.irifams.enums.ServiceType;
import suza.irifams.notification.NotificationService;
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
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;
    private final EmailService emailService;


    /*
     * =========================================================
     * SERVICE PRICES
     * =========================================================
     */

    private double getPricePerUnit(ServiceType serviceType) {

        return switch (serviceType) {

            case KUBURUGIWA -> 15000.0;
            case KUCHIMBA -> 20000.0;
            case KUVUNA -> 30000.0;
            case DAWA_CHUPA -> 25000.0;
            case DAWA_VIFUKO_KUBWA -> 30000.0;
            case DAWA_VIFUKO_NDOGO -> 12000.0;
            case KUTILIWA_DAWA -> 10000.0;
            case MBOLEA -> 1500.0;
            case MBEGU_MPUNGA -> 6000.0;
        };
    }


    /*
     * =========================================================
     * FARMER SUBMITS REQUEST
     * =========================================================
     */

    @PreAuthorize("hasRole('FARMER')")
    @PostMapping
    public ResponseEntity<?> submitRequest(

            @RequestParam Long farmerId,
            @RequestParam Long plotId,
            @RequestBody ServiceRequest request,
            Authentication authentication

    ) {

        User actor = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();


        /*
         * =====================================================
         * FARMER CHECK
         * =====================================================
         */

        User farmer = userRepository
                .findById(farmerId)
                .orElse(null);


        if (farmer == null) {

            auditLogger.log(
                    actor.getUsername(),
                    "REQUEST SUBMITTED",
                    "REQUEST",
                    "Farmer not found",
                    "FAILED"
            );

            return ResponseEntity
                    .badRequest()
                    .body("Farmer not found");
        }


        /*
         * =====================================================
         * SECURITY CHECK
         *
         * Logged in farmer must be the same farmerId.
         * =====================================================
         */

        if (!actor.getId().equals(farmer.getId())) {

            auditLogger.log(
                    actor.getUsername(),
                    "REQUEST SUBMITTED",
                    "REQUEST",
                    "Unauthorized farmer ID attempt",
                    "FAILED"
            );

            return ResponseEntity
                    .status(403)
                    .body("You can only submit requests for yourself");
        }


        /*
         * =====================================================
         * PLOT CHECK
         * =====================================================
         */

        Plot plot = plotRepository
                .findById(plotId)
                .orElse(null);


        if (plot == null) {

            auditLogger.log(
                    actor.getUsername(),
                    "REQUEST SUBMITTED",
                    "REQUEST",
                    "Plot not found",
                    "FAILED"
            );

            return ResponseEntity
                    .badRequest()
                    .body("Plot not found");
        }


        /*
         * =====================================================
         * PLOT OWNERSHIP CHECK
         * =====================================================
         */

        if (plot.getFarmer() == null
                || !plot.getFarmer()
                .getId()
                .equals(farmer.getId())) {

            auditLogger.log(
                    actor.getUsername(),
                    "REQUEST SUBMITTED",
                    "REQUEST",
                    "Farmer attempted to submit request for another farmer's plot",
                    "FAILED"
            );

            return ResponseEntity
                    .status(403)
                    .body(
                            "You can only submit requests for your own plots"
                    );
        }


        /*
         * =====================================================
         * SERVICE TYPE CHECK
         * =====================================================
         */

        if (request.getServiceType() == null) {

            auditLogger.log(
                    actor.getUsername(),
                    "REQUEST SUBMITTED",
                    "REQUEST",
                    "Service type was not provided",
                    "FAILED"
            );

            return ResponseEntity
                    .badRequest()
                    .body("Service type is required");
        }


        /*
         * =====================================================
         * SET REQUEST DATA
         * =====================================================
         */

        request.setFarmer(farmer);

        request.setPlot(plot);

        request.setStatus(
                RequestStatus.PENDING
        );

        request.setPaymentConfirmed(
                false
        );

        request.setCreatedAt(
                LocalDateTime.now()
        );


        /*
         * Amount is calculated only after approval.
         */

        request.setAmount(null);

        request.setControlNumber(null);


        /*
         * =====================================================
         * SAVE REQUEST
         * =====================================================
         */

        ServiceRequest savedRequest =
                repository.save(request);


        /*
         * =====================================================
         * NOTIFY FARMER
         * =====================================================
         */

        notificationService.notify(
                farmer,
                "Your "
                        + savedRequest.getServiceType()
                        + " service request has been submitted successfully "
                        + "and is awaiting review."
        );


        /*
         * =====================================================
         * NOTIFY SUPERVISORS
         * =====================================================
         */

        if (plot.getBlock() != null) {

            userRepository.findByRoleAndBlockName(
                    Role.SUPERVISOR,
                    plot.getBlock()
            ).forEach(supervisor ->

                    notificationService.notify(
                            supervisor,
                            "New "
                                    + savedRequest.getServiceType()
                                    + " request has been submitted for Plot "
                                    + plot.getPlotNo()
                    )
            );
        }


        /*
         * =====================================================
         * NOTIFY ADMINS
         * =====================================================
         */

        userRepository.findByRole(Role.ADMIN)
                .forEach(admin ->

                        notificationService.notify(
                                admin,
                                "New service request #"
                                        + savedRequest.getId()
                                        + " submitted by "
                                        + farmer.getFullName()
                        )
                );


        /*
         * =====================================================
         * AUDIT
         * =====================================================
         */

        auditLogger.log(
                actor.getUsername(),
                "REQUEST SUBMITTED",
                "REQUEST",
                "Submitted "
                        + savedRequest.getServiceType()
                        + " request for Plot "
                        + plot.getPlotNo(),
                "SUCCESS"
        );


        return ResponseEntity.ok(
                savedRequest
        );
    }


    /*
     * =========================================================
     * GET ALL REQUESTS
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @GetMapping
    public List<ServiceRequest> getAllRequests() {

        return repository.findAll();
    }


    /*
     * =========================================================
     * GET FARMER REQUESTS BY FARMER ID
     *
     * ADMIN/SUPERVISOR can view.
     * FARMER can only view own requests.
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('FARMER','ADMIN','SUPERVISOR')")
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<?> getFarmerRequests(
            @PathVariable Long farmerId,
            Authentication authentication
    ) {

        User actor = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();


        if (actor.getRole() == Role.FARMER
                && !actor.getId().equals(farmerId)) {

            return ResponseEntity
                    .status(403)
                    .body(
                            "You can only view your own requests"
                    );
        }


        return ResponseEntity.ok(
                repository.findByFarmerId(farmerId)
        );
    }


    /*
     * =========================================================
     * APPROVE REQUEST
     * =========================================================
     *
     * FLOW:
     *
     * 1. Check supervisor block
     * 2. Check request status
     * 3. Calculate official amount
     * 4. Generate control number
     * 5. Change status to WAITING_PAYMENT
     * 6. Save
     * 7. Notify farmer/admin
     * 8. Send email
     */

    /*
     * =========================================================
     * APPROVE REQUEST
     * =========================================================
     *
     * FLOW:
     *
     * 1. Check supervisor block
     * 2. Check request status
     * 3. Check service type
     * 4. Get confirmed quantity from frontend
     * 5. Calculate official amount
     * 6. Generate control number
     * 7. Change status to WAITING_PAYMENT
     * 8. Save
     * 9. Notify farmer/admin
     * 10. Send email
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveRequest(

            @PathVariable Long id,

            @RequestBody(
                    required = false
            )
            java.util.Map<String, Object> payload,

            Authentication authentication

    ) {

        User actor = userRepository
                .findByUsername(
                        authentication.getName()
                )
                .orElseThrow();


        return repository.findById(id)

                .map(req -> {


                    /*
                     * =================================================
                     * SUPERVISOR BLOCK CHECK
                     * =================================================
                     */

                    if (actor.getRole() == Role.SUPERVISOR) {

                        if (req.getPlot() == null
                                || req.getPlot().getBlock() == null
                                || actor.getBlockName() == null
                                || !req.getPlot()
                                .getBlock()
                                .equalsIgnoreCase(
                                        actor.getBlockName()
                                )) {

                            auditLogger.log(
                                    actor.getUsername(),
                                    "REQUEST APPROVED",
                                    "REQUEST",
                                    "Unauthorized approval attempt for Request #"
                                            + req.getId(),
                                    "FAILED"
                            );

                            return ResponseEntity
                                    .status(403)
                                    .body(
                                            "You can only manage requests within your block"
                                    );
                        }
                    }


                    /*
                     * =================================================
                     * STATUS CHECK
                     * =================================================
                     */

                    if (req.getStatus()
                            != RequestStatus.PENDING) {

                        return ResponseEntity
                                .badRequest()
                                .body(
                                        "Only pending requests can be approved"
                                );
                    }


                    /*
                     * =================================================
                     * SERVICE TYPE CHECK
                     * =================================================
                     */

                    if (req.getServiceType() == null) {

                        auditLogger.log(
                                actor.getUsername(),
                                "REQUEST APPROVED",
                                "REQUEST",
                                "Service type is missing for Request #"
                                        + req.getId(),
                                "FAILED"
                        );

                        return ResponseEntity
                                .badRequest()
                                .body(
                                        "Service type is required"
                                );
                    }


                    /*
                     * =================================================
                     * GET PRICE PER UNIT
                     * =================================================
                     */

                    double pricePerUnit =
                            getPricePerUnit(
                                    req.getServiceType()
                            );


                    double amount;


                    /*
                     * =================================================
                     * TRACTOR / MACHINE SERVICES
                     * =================================================
                     *
                     * Price is calculated automatically
                     * based on Plot Size.
                     *
                     * 1/4 Acre = 1 Unit
                     *
                     * Quantity confirmation is NOT required.
                     */

                    if (req.getServiceType()
                            == ServiceType.KUBURUGIWA

                            || req.getServiceType()
                            == ServiceType.KUCHIMBA

                            || req.getServiceType()
                            == ServiceType.KUVUNA) {


                        /*
                         * PLOT SIZE CHECK
                         */

                        if (req.getPlot() == null
                                || req.getPlot().getSize() == null
                                || req.getPlot().getSize() <= 0) {

                            auditLogger.log(
                                    actor.getUsername(),
                                    "REQUEST APPROVED",
                                    "REQUEST",
                                    "Plot size missing for tractor service Request #"
                                            + req.getId(),
                                    "FAILED"
                            );

                            return ResponseEntity
                                    .badRequest()
                                    .body(
                                            "Plot size is required for tractor services"
                                    );
                        }


                        /*
                         * CALCULATE UNITS
                         *
                         * Example:
                         *
                         * 1 Acre
                         *
                         * 1 / 0.25 = 4 Units
                         */

                        double units =
                                Math.ceil(
                                        req.getPlot()
                                                .getSize()
                                                / 0.25
                                );


                        /*
                         * CALCULATE AMOUNT
                         */

                        amount =
                                pricePerUnit
                                        * units;
                    }


                    /*
                     * =================================================
                     * QUANTITY BASED SERVICES
                     * =================================================
                     *
                     * Supervisor confirms quantity.
                     *
                     * Amount =
                     *
                     * Confirmed Quantity
                     * ×
                     * Price Per Unit
                     */

                    else {


                        /*
                         * CHECK PAYLOAD
                         */

                        if (payload == null
                                || !payload.containsKey(
                                "quantity"
                        )) {

                            return ResponseEntity
                                    .badRequest()
                                    .body(
                                            "Confirmed quantity is required"
                                    );
                        }


                        /*
                         * GET QUANTITY
                         */

                        Object quantityValue =
                                payload.get(
                                        "quantity"
                                );


                        if (quantityValue == null) {

                            return ResponseEntity
                                    .badRequest()
                                    .body(
                                            "Confirmed quantity is required"
                                    );
                        }


                        double confirmedQuantity;


                        /*
                         * CONVERT QUANTITY
                         */

                        try {

                            confirmedQuantity =
                                    Double.parseDouble(
                                            quantityValue.toString()
                                    );

                        } catch (Exception e) {

                            return ResponseEntity
                                    .badRequest()
                                    .body(
                                            "Invalid quantity"
                                    );
                        }


                        /*
                         * VALIDATE QUANTITY
                         */

                        if (confirmedQuantity <= 0) {

                            return ResponseEntity
                                    .badRequest()
                                    .body(
                                            "Quantity must be greater than zero"
                                    );
                        }


                        /*
                         * SAVE CONFIRMED QUANTITY
                         *
                         * Assuming quantity field is numeric.
                         */

                        req.setQuantity(
                                confirmedQuantity
                        );


                        /*
                         * CALCULATE FINAL AMOUNT
                         */

                        amount =
                                pricePerUnit
                                        * confirmedQuantity;
                    }


                    /*
                     * =================================================
                     * SET FINAL AMOUNT
                     * =================================================
                     */

                    req.setAmount(
                            amount
                    );


                    /*
                     * =================================================
                     * GENERATE CONTROL NUMBER
                     * =================================================
                     */

                    req.setControlNumber(
                            "CN-"
                                    + System.currentTimeMillis()
                                    + "-"
                                    + req.getId()
                    );


                    /*
                     * =================================================
                     * CHANGE STATUS
                     * =================================================
                     */

                    req.setStatus(
                            RequestStatus.WAITING_PAYMENT
                    );


                    /*
                     * =================================================
                     * PAYMENT NOT CONFIRMED YET
                     * =================================================
                     */

                    req.setPaymentConfirmed(
                            false
                    );


                    /*
                     * =================================================
                     * SAVE REQUEST
                     * =================================================
                     */

                    ServiceRequest saved =
                            repository.save(
                                    req
                            );


                    /*
                     * =================================================
                     * AUDIT LOG
                     * =================================================
                     */

                    auditLogger.log(
                            actor.getUsername(),
                            "REQUEST APPROVED",
                            "REQUEST",
                            "Approved Request #"
                                    + saved.getId()
                                    + " for "
                                    + saved.getServiceType()
                                    + ". Quantity: "
                                    + saved.getQuantity()
                                    + ". Amount: TZS "
                                    + saved.getAmount()
                                    + ", Control Number: "
                                    + saved.getControlNumber(),
                            "SUCCESS"
                    );


                    /*
                     * =================================================
                     * NOTIFY FARMER
                     * =================================================
                     */

                    notificationService.notify(
                            saved.getFarmer(),
                            "Your Request #"
                                    + saved.getId()
                                    + " has been approved. "
                                    + "Amount: TZS "
                                    + saved.getAmount()
                                    + ". Control Number: "
                                    + saved.getControlNumber()
                                    + ". Please proceed with payment."
                    );


                    /*
                     * =================================================
                     * NOTIFY ACTOR
                     * =================================================
                     */

                    notificationService.notify(
                            actor,
                            "You approved Request #"
                                    + saved.getId()
                    );


                    /*
                     * =================================================
                     * NOTIFY ADMINS
                     * =================================================
                     */

                    userRepository
                            .findByRole(
                                    Role.ADMIN
                            )
                            .forEach(admin -> {

                                if (!admin.getId()
                                        .equals(
                                                actor.getId()
                                        )) {

                                    notificationService.notify(
                                            admin,
                                            "Request #"
                                                    + saved.getId()
                                                    + " has been approved by "
                                                    + actor.getFullName()
                                    );
                                }

                            });


                    /*
                     * =================================================
                     * SEND APPROVAL EMAIL
                     * =================================================
                     */

                    try {

                        if (saved.getFarmer() != null
                                && saved.getFarmer()
                                .getEmail() != null
                                && !saved.getFarmer()
                                .getEmail()
                                .isBlank()) {

                            emailService
                                    .sendRequestApprovalEmail(
                                            saved
                                    );


                            auditLogger.log(
                                    actor.getUsername(),
                                    "SEND APPROVAL EMAIL",
                                    "EMAIL",
                                    "Approval email sent successfully for Request #"
                                            + saved.getId()
                                            + " to "
                                            + saved.getFarmer()
                                            .getEmail(),
                                    "SUCCESS"
                            );
                        }

                    } catch (Exception e) {

                        auditLogger.log(
                                actor.getUsername(),
                                "SEND APPROVAL EMAIL",
                                "EMAIL",
                                "Request #"
                                        + saved.getId()
                                        + " was approved successfully but approval email could not be sent.",
                                "FAILED"
                        );
                    }


                    /*
                     * =================================================
                     * RETURN APPROVED REQUEST
                     * =================================================
                     */

                    return ResponseEntity.ok(
                            saved
                    );

                })

                .orElseGet(() -> {


                    auditLogger.log(
                            actor.getUsername(),
                            "REQUEST APPROVED",
                            "REQUEST",
                            "Request #" + id + " not found",
                            "FAILED"
                    );


                    return ResponseEntity
                            .notFound()
                            .build();

                });
    }


    /*
     * =========================================================
     * REJECT REQUEST
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User actor = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();


        return repository.findById(id)

                .map(req -> {


                    /*
                     * =================================================
                     * SUPERVISOR BLOCK CHECK
                     * =================================================
                     */

                    if (actor.getRole() == Role.SUPERVISOR) {

                        if (req.getPlot() == null
                                || req.getPlot().getBlock() == null
                                || actor.getBlockName() == null
                                || !req.getPlot()
                                .getBlock()
                                .equalsIgnoreCase(
                                        actor.getBlockName()
                                )) {

                            auditLogger.log(
                                    actor.getUsername(),
                                    "REQUEST REJECTED",
                                    "REQUEST",
                                    "Unauthorized rejection attempt for Request #"
                                            + req.getId(),
                                    "FAILED"
                            );

                            return ResponseEntity
                                    .status(403)
                                    .body(
                                            "You can only manage requests within your block"
                                    );
                        }
                    }


                    /*
                     * =================================================
                     * STATUS CHECK
                     * =================================================
                     */

                    if (req.getStatus()
                            != RequestStatus.PENDING) {

                        return ResponseEntity
                                .badRequest()
                                .body(
                                        "Only pending requests can be rejected"
                                );
                    }


                    /*
                     * =================================================
                     * REJECT
                     * =================================================
                     */

                    req.setStatus(
                            RequestStatus.REJECTED
                    );


                    ServiceRequest saved =
                            repository.save(req);


                    /*
                     * =================================================
                     * AUDIT
                     * =================================================
                     */

                    auditLogger.log(
                            actor.getUsername(),
                            "REQUEST REJECTED",
                            "REQUEST",
                            "Rejected Request #"
                                    + saved.getId(),
                            "SUCCESS"
                    );


                    /*
                     * =================================================
                     * NOTIFY FARMER
                     * =================================================
                     */

                    notificationService.notify(
                            saved.getFarmer(),
                            "Your Request #"
                                    + saved.getId()
                                    + " has been rejected."
                    );


                    /*
                     * =================================================
                     * NOTIFY ADMINS
                     * =================================================
                     */

                    userRepository.findByRole(Role.ADMIN)
                            .forEach(admin -> {

                                if (!admin.getId()
                                        .equals(actor.getId())) {

                                    notificationService.notify(
                                            admin,
                                            "Request #"
                                                    + saved.getId()
                                                    + " has been rejected by "
                                                    + actor.getFullName()
                                    );
                                }
                            });


                    /*
                     * =================================================
                     * NOTIFY ACTOR
                     * =================================================
                     */

                    notificationService.notify(
                            actor,
                            "You rejected Request #"
                                    + saved.getId()
                    );


                    return ResponseEntity.ok(
                            saved
                    );

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            actor.getUsername(),
                            "REQUEST REJECTED",
                            "REQUEST",
                            "Request #" + id + " not found",
                            "FAILED"
                    );

                    return ResponseEntity
                            .notFound()
                            .build();
                });
    }


    /*
     * =========================================================
     * GET SUPERVISOR REQUESTS
     * =========================================================
     */

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/my-requests")
    public List<ServiceRequest> getMyRequests(
            Authentication authentication
    ) {

        User supervisor =
                userRepository
                        .findByUsername(
                                authentication.getName()
                        )
                        .orElseThrow();


        return repository
                .findByPlotBlockOrderByCreatedAtDesc(
                        supervisor.getBlockName()
                );
    }


    /*
     * =========================================================
     * GET FARMER OWN REQUESTS
     * =========================================================
     */

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/my-farm-requests")
    public List<ServiceRequest> getMyFarmRequests(
            Authentication authentication
    ) {

        User farmer =
                userRepository
                        .findByUsername(
                                authentication.getName()
                        )
                        .orElseThrow();


        return repository
                .findByFarmerId(
                        farmer.getId()
                );
    }


    /*
     * =========================================================
     * GET SINGLE REQUEST
     * =========================================================
     *
     * Farmer can only view own request.
     * Supervisor can only view request in own block.
     * Admin can view all.
     */

    @PreAuthorize("hasAnyRole('FARMER','ADMIN','SUPERVISOR')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User actor = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();


        return repository.findById(id)

                .map(request -> {


                    /*
                     * FARMER SECURITY
                     */

                    if (actor.getRole() == Role.FARMER) {

                        if (request.getFarmer() == null
                                || !request.getFarmer()
                                .getId()
                                .equals(actor.getId())) {

                            return ResponseEntity
                                    .status(403)
                                    .body("Access denied");
                        }
                    }


                    /*
                     * SUPERVISOR SECURITY
                     */

                    if (actor.getRole() == Role.SUPERVISOR) {

                        if (request.getPlot() == null
                                || request.getPlot().getBlock() == null
                                || actor.getBlockName() == null
                                || !request.getPlot()
                                .getBlock()
                                .equalsIgnoreCase(
                                        actor.getBlockName()
                                )) {

                            return ResponseEntity
                                    .status(403)
                                    .body("Access denied");
                        }
                    }


                    return ResponseEntity.ok(
                            request
                    );

                })

                .orElseGet(() ->
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }
}