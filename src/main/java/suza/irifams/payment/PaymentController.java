package suza.irifams.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.enums.PaymentStatus;
import suza.irifams.enums.RequestStatus;
import suza.irifams.enums.Role;
import suza.irifams.notification.Notification;
import suza.irifams.notification.NotificationRepository;
import suza.irifams.notification.NotificationService;
import suza.irifams.request.ServiceRequest;
import suza.irifams.request.ServiceRequestRepository;
import org.springframework.security.core.Authentication;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final ServiceRequestRepository requestRepository;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;
    private final UserRepository userRepository;

    /*
     * Farmer confirms payment
     */

    @PreAuthorize("hasRole('FARMER')")
    @PostMapping("/confirm/{requestId}")
    public ResponseEntity<?> confirmPayment(

            @PathVariable Long requestId,

            @RequestBody Payment payment

    ) {

        ServiceRequest request = requestRepository
                .findById(requestId)
                .orElseThrow();

        payment.setFarmer(request.getFarmer());

        payment.setServiceRequest(request);

        payment.setPaymentDate(LocalDateTime.now());

        payment.setStatus(
                PaymentStatus.WAITING_VERIFICATION
        );

        request.setStatus(
                RequestStatus.WAITING_VERIFICATION
        );

        requestRepository.save(request);

        auditLogger.log(
                request.getFarmer().getUsername(),
                "PAYMENT SUBMITTED",
                "PAYMENT",
                "Submitted payment for Request #" + request.getId(),
                "SUCCESS"
        );

        Payment savedPayment =
                paymentRepository.save(payment);

        // Notification to Farmer

        // Farmer
        notificationService.notify(
                request.getFarmer(),
                "Payment submitted successfully. Waiting for verification."
        );

// Supervisor wa block
        userRepository.findByRoleAndBlockName(
                Role.SUPERVISOR,
                request.getPlot().getBlock()
        ).forEach(supervisor ->

                notificationService.notify(
                        supervisor,
                        "New payment submitted for verification."
                )

        );

// Admins
        userRepository.findByRole(Role.ADMIN)
                .forEach(admin ->

                        notificationService.notify(
                                admin,
                                "A farmer has submitted a payment waiting for verification."
                        )

                );
        return ResponseEntity.ok(savedPayment);

    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','FARMER')")
    @GetMapping("/my-payments")
    public List<Payment> getMyPayments(
            Authentication authentication
    ) {

        String username = authentication.getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow();

        boolean isSupervisor =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(a ->
                                a.getAuthority().equals("ROLE_SUPERVISOR"));

        if (isSupervisor) {

            return paymentRepository
                    .findByFarmerBlockNameOrderByPaymentDateDesc(
                            user.getBlockName()
                    );

        }

        return paymentRepository
                .findByFarmerId(
                        user.getId()
                );
    }

    /*
     * Admin verifies payment
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/verify")
    public ResponseEntity<?> verifyPayment(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return paymentRepository.findById(id)

                .map(payment -> {

                    User actor = userRepository
                            .findByUsername(authentication.getName())
                            .orElseThrow();

                    // Supervisor anaweza verify block yake tu
                    if(actor.getRole() == Role.SUPERVISOR){

                        if(!payment.getFarmer()
                                .getBlockName()
                                .equals(actor.getBlockName())){

                            auditLogger.log(
                                    actor.getUsername(),
                                    "VERIFY PAYMENT",
                                    "PAYMENT",
                                    "Attempted to verify payment outside assigned block",
                                    "FAILED"
                            );

                            return ResponseEntity
                                    .status(403)
                                    .body("Access denied");
                        }
                    }

                    payment.setStatus(PaymentStatus.PAID);

                    paymentRepository.save(payment);

                    ServiceRequest request =
                            payment.getServiceRequest();

                    request.setStatus(RequestStatus.PAID);

                    requestRepository.save(request);

                    auditLogger.log(

                            actor.getUsername(),

                            "VERIFY PAYMENT",

                            "PAYMENT",

                            "Verified payment #"
                                    + payment.getId(),

                            "SUCCESS"

                    );

                    // Farmer
                    notificationService.notify(

                            payment.getFarmer(),

                            "Your payment has been verified successfully."

                    );

                    // Actor
                    notificationService.notify(

                            actor,

                            "You verified payment #"
                                    + payment.getId()

                    );

                    // Other admins
                    userRepository.findByRole(Role.ADMIN)

                            .stream()

                            .filter(admin ->

                                    !admin.getId()
                                            .equals(actor.getId())

                            )

                            .forEach(admin ->

                                    notificationService.notify(

                                            admin,

                                            "Payment #"
                                                    + payment.getId()
                                                    + " has been verified."

                                    )

                            );

                    return ResponseEntity.ok(payment);

                })

                .orElseGet(() -> {

                    auditLogger.log(

                            "UNKNOWN",

                            "VERIFY PAYMENT",

                            "PAYMENT",

                            "Payment not found",

                            "FAILED"

                    );

                    return ResponseEntity.notFound().build();

                });

    }

    /*
     * Farmer payments
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR','FARMER')")
    @GetMapping("/farmer/{farmerId}")
    public List<Payment> getFarmerPayments(
            @PathVariable Long farmerId
    ) {

        return paymentRepository
                .findByFarmerId(farmerId);

    }

    /*
     * All payments
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @GetMapping
    public List<Payment> getAllPayments() {

        return paymentRepository.findAll();

    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectPayment(
            @PathVariable Long id,
            Authentication authentication
    ){

        return paymentRepository.findById(id)

                .map(payment -> {

                    User actor = userRepository
                            .findByUsername(authentication.getName())
                            .orElseThrow();

                    payment.setStatus(
                            PaymentStatus.REJECTED
                    );

                    paymentRepository.save(payment);

                    ServiceRequest request =
                            payment.getServiceRequest();

                    request.setStatus(
                            RequestStatus.WAITING_PAYMENT
                    );

                    requestRepository.save(request);

                    auditLogger.log(

                            actor.getUsername(),

                            "REJECT PAYMENT",

                            "PAYMENT",

                            "Rejected payment #"
                                    + payment.getId(),

                            "SUCCESS"

                    );

                    notificationService.notify(

                            payment.getFarmer(),

                            "Your payment has been rejected. Please re-submit payment details."

                    );

                    notificationService.notify(

                            actor,

                            "You rejected payment #"
                                    + payment.getId()

                    );

                    userRepository.findByRole(Role.ADMIN)

                            .stream()

                            .filter(admin ->

                                    !admin.getId()
                                            .equals(actor.getId())

                            )

                            .forEach(admin ->

                                    notificationService.notify(

                                            admin,

                                            "Payment #"
                                                    + payment.getId()
                                                    + " has been rejected."

                                    )

                            );

                    return ResponseEntity.ok(payment);

                })

                .orElseGet(() -> {

                    auditLogger.log(

                            "UNKNOWN",

                            "REJECT PAYMENT",

                            "PAYMENT",

                            "Payment not found",

                            "FAILED"

                    );

                    return ResponseEntity.notFound().build();

                });

    }
}