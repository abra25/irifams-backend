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
                "PAYMENT_CONFIRM",
                "PAYMENT",
                "Farmer confirmed payment"
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

    /*
     * Admin verifies payment
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/verify")
    public ResponseEntity<?> verifyPayment(
            @PathVariable Long id
    ) {

        return paymentRepository.findById(id)

                .map(payment -> {

                    payment.setStatus(
                            PaymentStatus.PAID
                    );

                    paymentRepository.save(payment);

                    ServiceRequest request =
                            payment.getServiceRequest();

                    request.setStatus(
                            RequestStatus.COMPLETED
                    );

                    requestRepository.save(request);

                    auditLogger.log(
                            payment.getFarmer().getUsername(),
                            "PAYMENT_VERIFIED",
                            "PAYMENT",
                            "Payment verified successfully"
                    );

                    // Notification to Farmer

                    notificationRepository.save(

                            Notification.builder()

                                    .message(
                                            "Your payment has been verified successfully."
                                    )

                                    .user(request.getFarmer())

                                    .isRead(false)

                                    .createdAt(LocalDateTime.now())

                                    .build()

                    );

                    return ResponseEntity.ok(payment);

                })

                .orElse(
                        ResponseEntity.notFound().build()
                );
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

}