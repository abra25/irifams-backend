package suza.irifams.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.enums.PaymentStatus;
import suza.irifams.enums.RequestStatus;
import suza.irifams.notification.Notification;
import suza.irifams.notification.NotificationRepository;
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
    private final NotificationRepository notificationRepository;
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
                "Farmer confirmed payment",
                "SUCCESS"
        );

        Payment savedPayment =
                paymentRepository.save(payment);

        // Notification to Farmer

        notificationRepository.save(

                Notification.builder()

                        .message(
                                "Payment submitted successfully. Waiting for verification."
                        )

                        .user(request.getFarmer())

                        .isRead(false)

                        .createdAt(LocalDateTime.now())

                        .build()

        );

        return ResponseEntity.ok(savedPayment);

    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','FARMER')")
    @GetMapping("/my-payments")
    public List<Payment> getMyPayments(
            Authentication authentication
    ){

        String username = authentication.getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow();

        boolean isSupervisor =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(a ->
                                a.getAuthority().equals("ROLE_SUPERVISOR"));

        if(isSupervisor){

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

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/verify")
    public ResponseEntity<?> verifyPayment(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return paymentRepository.findById(id)

                .map(payment -> {

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

                        if(!payment.getFarmer()

                                .getBlockName()

                                .equals(
                                        supervisor.getBlockName()
                                )){

                            auditLogger.log(
                                    supervisor.getUsername(),
                                    "PAYMENT VERIFIED",
                                    "PAYMENT",
                                    "Unauthorized payment verification attempt",
                                    "FAILED"
                            );

                            return ResponseEntity
                                    .status(403)
                                    .body("Access denied");

                        }

                    }

                    payment.setStatus(
                            PaymentStatus.PAID
                    );

                    paymentRepository.save(payment);

                    ServiceRequest request =
                            payment.getServiceRequest();

                    request.setStatus(
                            RequestStatus.PAID
                    );

                    requestRepository.save(request);

                    auditLogger.log(
                            payment.getFarmer()
                                    .getUsername(),

                            "PAYMENT VERIFIED",

                            "PAYMENT",

                            "Payment verified successfully",

                            "SUCCESS"
                    );

                    notificationRepository.save(

                            Notification.builder()

                                    .message(
                                            "Your payment has been verified successfully."
                                    )

                                    .user(
                                            request.getFarmer()
                                    )

                                    .isRead(false)

                                    .createdAt(
                                            LocalDateTime.now()
                                    )

                                    .build()

                    );

                    return ResponseEntity.ok(payment);

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            "UNKNOWN",
                            "PAYMENT VERIFIED",
                            "PAYMENT",
                            "Failed to verify payment",
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

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectPayment(
            @PathVariable Long id
    ) {

        return paymentRepository.findById(id)

                .map(payment -> {

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
                            payment.getFarmer()
                                    .getUsername(),

                            "PAYMENT REJECTED",

                            "PAYMENT",

                            "Payment rejected",

                            "SUCCESS"
                    );

                    notificationRepository.save(

                            Notification.builder()

                                    .message(
                                            "Your payment has been rejected. Please re-submit payment details."
                                    )

                                    .user(
                                            payment.getFarmer()
                                    )

                                    .isRead(false)

                                    .createdAt(
                                            LocalDateTime.now()
                                    )

                                    .build()

                    );

                    return ResponseEntity.ok(payment);

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            "UNKNOWN",

                            "PAYMENT REJECTED",

                            "PAYMENT",

                            "Failed to reject payment",

                            "FAILED"
                    );

                    return ResponseEntity.notFound().build();

                });

    }

//    @PreAuthorize("hasRole('FARMER')")
//    @GetMapping("/my-payments")
//    public List<Payment> getFarmerPayments(
//
//            Authentication authentication
//
//    ){
//
//        String username=
//
//                authentication.getName();
//
//        User farmer=
//
//                userRepository
//
//                        .findByUsername(username)
//
//                        .orElseThrow();
//
//        return paymentRepository
//
//                .findByFarmerId(
//
//                        farmer.getId()
//
//                );
//
//    }

}