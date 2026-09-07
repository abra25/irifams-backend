package suza.irifams.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import suza.irifams.audit.AuditLogger;
import suza.irifams.email.EmailService;
import suza.irifams.email.ReceiptPdfService;
import suza.irifams.enums.PaymentStatus;
import suza.irifams.enums.RequestStatus;
import suza.irifams.enums.Role;
import suza.irifams.notification.NotificationService;
import suza.irifams.request.ServiceRequest;
import suza.irifams.request.ServiceRequestRepository;
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
     * Email services
     */
    private final EmailService emailService;
    private final ReceiptPdfService receiptPdfService;


    /*
     * =========================================================
     * FARMER SUBMITS PAYMENT
     * =========================================================
     *
     * Farmer provides:
     * - Control Number
     *
     * Backend gets:
     * - Farmer from logged-in account
     * - Amount from ServiceRequest
     * - ServiceRequest from requestId
     *
     * Farmer cannot control the amount.
     */

    @PreAuthorize("hasRole('FARMER')")
    @PostMapping("/confirm/{requestId}")
    public ResponseEntity<?> confirmPayment(

            @PathVariable Long requestId,

            @RequestBody Payment payment,

            Authentication authentication

    ) {

        /*
         * =====================================================
         * GET LOGGED-IN FARMER
         * =====================================================
         */

        User farmer = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();


        /*
         * =====================================================
         * FIND SERVICE REQUEST
         * =====================================================
         */

        ServiceRequest request =
                requestRepository
                        .findById(requestId)
                        .orElse(null);


        if (request == null) {

            auditLogger.log(
                    farmer.getUsername(),
                    "PAYMENT SUBMITTED",
                    "PAYMENT",
                    "Service request not found",
                    "FAILED"
            );

            return ResponseEntity
                    .notFound()
                    .build();
        }


        /*
         * =====================================================
         * SECURITY CHECK
         * =====================================================
         *
         * Farmer can only pay for own request.
         */

        if (request.getFarmer() == null
                || !request.getFarmer()
                .getId()
                .equals(farmer.getId())) {

            auditLogger.log(
                    farmer.getUsername(),
                    "PAYMENT SUBMITTED",
                    "PAYMENT",
                    "Unauthorized payment attempt for Request #"
                            + request.getId(),
                    "FAILED"
            );

            return ResponseEntity
                    .status(403)
                    .body(
                            "You can only make payment for your own request"
                    );
        }


        /*
         * =====================================================
         * REQUEST STATUS CHECK
         * =====================================================
         *
         * Payment is only allowed after approval.
         */

        if (request.getStatus()
                != RequestStatus.WAITING_PAYMENT) {

            auditLogger.log(
                    farmer.getUsername(),
                    "PAYMENT SUBMITTED",
                    "PAYMENT",
                    "Request #" + request.getId()
                            + " is not waiting for payment",
                    "FAILED"
            );

            return ResponseEntity
                    .badRequest()
                    .body(
                            "This request is not waiting for payment"
                    );
        }


        /*
         * =====================================================
         * CONTROL NUMBER REQUIRED
         * =====================================================
         */

        if (payment.getControlNumber() == null
                || payment.getControlNumber().isBlank()) {

            auditLogger.log(
                    farmer.getUsername(),
                    "PAYMENT SUBMITTED",
                    "PAYMENT",
                    "Control number was not provided",
                    "FAILED"
            );

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Control number is required"
                    );
        }


        /*
         * =====================================================
         * CONTROL NUMBER VALIDATION
         * =====================================================
         *
         * Farmer's control number must match the one
         * generated by IRIFAMS.
         */

        if (!payment.getControlNumber()
                .trim()
                .equalsIgnoreCase(
                        request.getControlNumber()
                )) {

            auditLogger.log(
                    farmer.getUsername(),
                    "PAYMENT SUBMITTED",
                    "PAYMENT",
                    "Invalid control number for Request #"
                            + request.getId(),
                    "FAILED"
            );

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid control number. Payment failed."
                    );
        }


        /*
         * =====================================================
         * CHECK DUPLICATE PAYMENT
         * =====================================================
         */

        Payment existingPayment =
                paymentRepository
                        .findByControlNumber(
                                request.getControlNumber()
                        )
                        .orElse(null);


        if (existingPayment != null
                && existingPayment.getStatus()
                != PaymentStatus.REJECTED) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Payment has already been submitted for this control number"
                    );
        }


        /*
         * =====================================================
         * VALIDATE REQUEST AMOUNT
         * =====================================================
         *
         * Amount must already exist because the request
         * was approved by Supervisor/Admin.
         */

        if (request.getAmount() == null
                || request.getAmount() <= 0) {

            auditLogger.log(
                    farmer.getUsername(),
                    "PAYMENT SUBMITTED",
                    "PAYMENT",
                    "Invalid request amount for Request #"
                            + request.getId(),
                    "FAILED"
            );

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid payment amount"
                    );
        }


        /*
         * =====================================================
         * SET PAYMENT INFORMATION
         * =====================================================
         */

        payment.setFarmer(farmer);

        payment.setServiceRequest(request);

        /*
         * Always use the official control number
         * from the request.
         */
        payment.setControlNumber(
                request.getControlNumber()
        );

        /*
         * Always use the official amount
         * from the request.
         */
        payment.setAmount(
                request.getAmount()
        );

        payment.setPaymentDate(
                LocalDateTime.now()
        );

        payment.setStatus(
                PaymentStatus.WAITING_VERIFICATION
        );


        /*
         * =====================================================
         * UPDATE REQUEST STATUS
         * =====================================================
         */

        request.setStatus(
                RequestStatus.WAITING_VERIFICATION
        );

        requestRepository.save(request);


        /*
         * =====================================================
         * SAVE PAYMENT
         * =====================================================
         */

        Payment savedPayment =
                paymentRepository.save(payment);


        /*
         * =====================================================
         * AUDIT
         * =====================================================
         */

        auditLogger.log(
                farmer.getUsername(),
                "PAYMENT SUBMITTED",
                "PAYMENT",
                "Submitted payment for Request #"
                        + request.getId()
                        + " using Control Number "
                        + request.getControlNumber()
                        + ". Amount: TZS "
                        + request.getAmount(),
                "SUCCESS"
        );


        /*
         * =====================================================
         * IN-APP NOTIFICATION - FARMER
         * =====================================================
         */

        notificationService.notify(
                farmer,
                "Payment submitted successfully for Request #"
                        + request.getId()
                        + ". Waiting for verification."
        );


        /*
         * =====================================================
         * IN-APP NOTIFICATION - SUPERVISORS
         * =====================================================
         */

        if (request.getPlot() != null) {

            userRepository.findByRoleAndBlockName(
                    Role.SUPERVISOR,
                    request.getPlot().getBlock()
            ).forEach(supervisor ->

                    notificationService.notify(
                            supervisor,
                            "New payment submitted by "
                                    + farmer.getFullName()
                                    + " for Request #"
                                    + request.getId()
                                    + ". Waiting for verification."
                    )
            );
        }


        /*
         * =====================================================
         * IN-APP NOTIFICATION - ADMINS
         * =====================================================
         */

        userRepository.findByRole(Role.ADMIN)
                .forEach(admin ->

                        notificationService.notify(
                                admin,
                                "Payment submitted for Request #"
                                        + request.getId()
                                        + " by "
                                        + farmer.getFullName()
                        )
                );


        return ResponseEntity.ok(savedPayment);
    }


    /*
     * =========================================================
     * GET MY PAYMENTS
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('SUPERVISOR','FARMER')")
    @GetMapping("/my-payments")
    public List<Payment> getMyPayments(
            Authentication authentication
    ) {

        String username =
                authentication.getName();


        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow();


        boolean isSupervisor =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(a ->
                                a.getAuthority()
                                        .equals(
                                                "ROLE_SUPERVISOR"
                                        )
                        );


        /*
         * Supervisor:
         * Payments belonging to farmers
         * in supervisor's block.
         */

        if (isSupervisor) {

            return paymentRepository
                    .findByFarmerBlockNameOrderByPaymentDateDesc(
                            user.getBlockName()
                    );
        }


        /*
         * Farmer:
         * Own payments only.
         */

        return paymentRepository
                .findByFarmerId(
                        user.getId()
                );
    }


    /*
     * =========================================================
     * VERIFY PAYMENT
     * =========================================================
     *
     * Admin or Supervisor verifies payment.
     *
     * Successful verification:
     *
     * WAITING_VERIFICATION
     *          ↓
     *        PAID
     *          ↓
     * Receipt Number generated
     *          ↓
     * PDF generated
     *          ↓
     * Email sent to Farmer
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/verify")
    public ResponseEntity<?> verifyPayment(
            @PathVariable Long id,
            Authentication authentication
    ) {

        /*
         * =====================================================
         * GET ACTOR
         * =====================================================
         */

        User actor =
                userRepository
                        .findByUsername(
                                authentication.getName()
                        )
                        .orElseThrow();


        return paymentRepository.findById(id)

                .map(payment -> {

                    /*
                     * =================================================
                     * SUPERVISOR BLOCK RESTRICTION
                     * =================================================
                     */

                    if (actor.getRole()
                            == Role.SUPERVISOR) {

                        if (payment.getFarmer() == null
                                || payment.getFarmer()
                                .getBlockName() == null
                                || !payment.getFarmer()
                                .getBlockName()
                                .equalsIgnoreCase(
                                        actor.getBlockName()
                                )) {

                            auditLogger.log(
                                    actor.getUsername(),
                                    "VERIFY PAYMENT",
                                    "PAYMENT",
                                    "Attempted to verify payment outside assigned block",
                                    "FAILED"
                            );

                            return ResponseEntity
                                    .status(403)
                                    .body(
                                            "Access denied"
                                    );
                        }
                    }


                    /*
                     * =================================================
                     * PAYMENT STATUS CHECK
                     * =================================================
                     */

                    if (payment.getStatus()
                            != PaymentStatus.WAITING_VERIFICATION) {

                        return ResponseEntity
                                .badRequest()
                                .body(
                                        "Only payments waiting for verification can be verified"
                                );
                    }


                    /*
                     * =================================================
                     * MARK PAYMENT AS PAID
                     * =================================================
                     */

                    payment.setStatus(
                            PaymentStatus.PAID
                    );


                    /*
                     * =================================================
                     * GENERATE RECEIPT NUMBER
                     * =================================================
                     */

                    if (payment.getReceiptNumber() == null
                            || payment.getReceiptNumber().isBlank()) {

                        payment.setReceiptNumber(
                                "RCT-"
                                        + System.currentTimeMillis()
                        );
                    }


                    /*
                     * Payment verification date
                     */

                    payment.setPaymentDate(
                            LocalDateTime.now()
                    );


                    /*
                     * =================================================
                     * SAVE PAYMENT
                     * =================================================
                     */

                    Payment savedPayment =
                            paymentRepository.save(
                                    payment
                            );


                    /*
                     * =================================================
                     * UPDATE SERVICE REQUEST
                     * =================================================
                     */

                    ServiceRequest request =
                            payment.getServiceRequest();


                    request.setStatus(
                            RequestStatus.PAID
                    );

                    request.setPaymentConfirmed(
                            true
                    );


                    requestRepository.save(
                            request
                    );


                    /*
                     * =================================================
                     * AUDIT - SUCCESS
                     * =================================================
                     */

                    auditLogger.log(
                            actor.getUsername(),
                            "VERIFY PAYMENT",
                            "PAYMENT",
                            "Verified payment #"
                                    + payment.getId()
                                    + " for Request #"
                                    + request.getId()
                                    + ". Receipt: "
                                    + savedPayment
                                    .getReceiptNumber(),
                            "SUCCESS"
                    );


                    /*
                     * =================================================
                     * IN-APP NOTIFICATION - FARMER
                     * =================================================
                     */

                    notificationService.notify(
                            payment.getFarmer(),
                            "Your payment for Request #"
                                    + request.getId()
                                    + " has been verified successfully. "
                                    + "Receipt Number: "
                                    + savedPayment
                                    .getReceiptNumber()
                    );


                    /*
                     * =================================================
                     * IN-APP NOTIFICATION - ACTOR
                     * =================================================
                     */

                    notificationService.notify(
                            actor,
                            "You verified payment #"
                                    + payment.getId()
                    );


                    /*
                     * =================================================
                     * IN-APP NOTIFICATION - OTHER ADMINS
                     * =================================================
                     */

                    userRepository.findByRole(Role.ADMIN)

                            .stream()

                            .filter(admin ->
                                    !admin.getId()
                                            .equals(
                                                    actor.getId()
                                            )
                            )

                            .forEach(admin ->

                                    notificationService.notify(
                                            admin,
                                            "Payment #"
                                                    + payment.getId()
                                                    + " has been verified by "
                                                    + actor.getFullName()
                                    )
                            );


                    /*
                     * =================================================
                     * GENERATE PDF RECEIPT
                     * =================================================
                     */

                    try {

                        byte[] receiptPdf =
                                receiptPdfService.generateReceipt(
                                        request,
                                        savedPayment
                                                .getReceiptNumber()
                                );


                        /*
                         * =================================================
                         * SEND RECEIPT EMAIL
                         * =================================================
                         */

                        emailService.sendPaymentReceiptEmail(
                                request,
                                savedPayment
                                        .getReceiptNumber(),
                                receiptPdf
                        );


                        /*
                         * Audit email success
                         */

                        auditLogger.log(
                                actor.getUsername(),
                                "SEND PAYMENT RECEIPT",
                                "EMAIL",
                                "Payment receipt email sent successfully for Payment #"
                                        + savedPayment.getId()
                                        + " to "
                                        + payment.getFarmer()
                                        .getEmail(),
                                "SUCCESS"
                        );


                    } catch (Exception e) {

                        /*
                         * Email/PDF failure should not
                         * undo successful payment verification.
                         */

                        auditLogger.log(
                                actor.getUsername(),
                                "SEND PAYMENT RECEIPT",
                                "EMAIL",
                                "Payment was verified but receipt email failed for Payment #"
                                        + savedPayment.getId(),
                                "FAILED"
                        );

                    }


                    /*
                     * =================================================
                     * RETURN SUCCESS
                     * =================================================
                     */

                    return ResponseEntity.ok(
                            savedPayment
                    );

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            actor.getUsername(),
                            "VERIFY PAYMENT",
                            "PAYMENT",
                            "Payment not found",
                            "FAILED"
                    );

                    return ResponseEntity
                            .notFound()
                            .build();
                });
    }


    /*
     * =========================================================
     * GET FARMER PAYMENTS
     * =========================================================
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR','FARMER')")
    @GetMapping("/farmer/{farmerId}")
    public List<Payment> getFarmerPayments(
            @PathVariable Long farmerId
    ) {

        return paymentRepository
                .findByFarmerId(
                        farmerId
                );
    }


    /*
     * =========================================================
     * GET ALL PAYMENTS
     * =========================================================
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @GetMapping
    public List<Payment> getAllPayments() {

        return paymentRepository
                .findAll();
    }


    /*
     * =========================================================
     * REJECT PAYMENT
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectPayment(
            @PathVariable Long id,
            Authentication authentication
    ) {

        /*
         * =====================================================
         * GET ACTOR
         * =====================================================
         */

        User actor =
                userRepository
                        .findByUsername(
                                authentication.getName()
                        )
                        .orElseThrow();


        return paymentRepository.findById(id)

                .map(payment -> {

                    /*
                     * =================================================
                     * SUPERVISOR BLOCK RESTRICTION
                     * =================================================
                     */

                    if (actor.getRole()
                            == Role.SUPERVISOR) {

                        if (payment.getFarmer() == null
                                || payment.getFarmer()
                                .getBlockName() == null
                                || !payment.getFarmer()
                                .getBlockName()
                                .equalsIgnoreCase(
                                        actor.getBlockName()
                                )) {

                            auditLogger.log(
                                    actor.getUsername(),
                                    "REJECT PAYMENT",
                                    "PAYMENT",
                                    "Attempted to reject payment outside assigned block",
                                    "FAILED"
                            );

                            return ResponseEntity
                                    .status(403)
                                    .body(
                                            "Access denied"
                                    );
                        }
                    }


                    /*
                     * =================================================
                     * PAYMENT STATUS CHECK
                     * =================================================
                     */

                    if (payment.getStatus()
                            != PaymentStatus.WAITING_VERIFICATION) {

                        return ResponseEntity
                                .badRequest()
                                .body(
                                        "Only payments waiting for verification can be rejected"
                                );
                    }


                    /*
                     * =================================================
                     * REJECT PAYMENT
                     * =================================================
                     */

                    payment.setStatus(
                            PaymentStatus.REJECTED
                    );


                    paymentRepository.save(
                            payment
                    );


                    /*
                     * =================================================
                     * RETURN REQUEST TO WAITING PAYMENT
                     * =================================================
                     */

                    ServiceRequest request =
                            payment.getServiceRequest();


                    request.setStatus(
                            RequestStatus.WAITING_PAYMENT
                    );

                    request.setPaymentConfirmed(
                            false
                    );


                    requestRepository.save(
                            request
                    );


                    /*
                     * =================================================
                     * AUDIT
                     * =================================================
                     */

                    auditLogger.log(
                            actor.getUsername(),
                            "REJECT PAYMENT",
                            "PAYMENT",
                            "Rejected payment #"
                                    + payment.getId()
                                    + " for Request #"
                                    + request.getId(),
                            "SUCCESS"
                    );


                    /*
                     * =================================================
                     * NOTIFY FARMER
                     * =================================================
                     */

                    notificationService.notify(
                            payment.getFarmer(),
                            "Your payment for Request #"
                                    + request.getId()
                                    + " has been rejected. "
                                    + "Please submit the payment again."
                    );


                    /*
                     * =================================================
                     * NOTIFY ACTOR
                     * =================================================
                     */

                    notificationService.notify(
                            actor,
                            "You rejected payment #"
                                    + payment.getId()
                    );


                    /*
                     * =================================================
                     * NOTIFY ADMINS
                     * =================================================
                     */

                    userRepository.findByRole(Role.ADMIN)

                            .stream()

                            .filter(admin ->
                                    !admin.getId()
                                            .equals(
                                                    actor.getId()
                                            )
                            )

                            .forEach(admin ->

                                    notificationService.notify(
                                            admin,
                                            "Payment #"
                                                    + payment.getId()
                                                    + " has been rejected by "
                                                    + actor.getFullName()
                                    )
                            );


                    /*
                     * =================================================
                     * RETURN RESPONSE
                     * =================================================
                     */

                    return ResponseEntity.ok(
                            payment
                    );

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            actor.getUsername(),
                            "REJECT PAYMENT",
                            "PAYMENT",
                            "Payment not found",
                            "FAILED"
                    );

                    return ResponseEntity
                            .notFound()
                            .build();
                });
    }
}