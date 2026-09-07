package suza.irifams.security.auth;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import suza.irifams.audit.AuditLogger;
import suza.irifams.email.EmailService;
import suza.irifams.enums.Role;
import suza.irifams.notification.NotificationService;
import suza.irifams.security.service.JwtService;
import suza.irifams.security.util.OtpGenerator;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final OtpVerificationRepository otpRepository;


    // =========================================================
    // REGISTER
    // =========================================================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {

            auditLogger.log(
                    request.getUsername(),
                    "REGISTER",
                    "USER",
                    "Registration failed. Username already exists",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body("Username already exists");
        }

        User user = User.builder()
                .employeeNo(request.getEmployeeNo())
                .fullName(request.getFullName())
                .username(request.getUsername())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .gender(request.getGender())
                .blockName(request.getBlockName())
                .institution(request.getInstitution())
                .role(request.getRole())
                .image(request.getImage())
                .temporaryPassword(false)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        auditLogger.log(
                savedUser.getUsername(),
                "REGISTER",
                "USER",
                "New user account registered",
                "SUCCESS"
        );

        try {

            emailService.sendAccountCreatedEmail(savedUser);

            auditLogger.log(
                    savedUser.getUsername(),
                    "SEND ACCOUNT EMAIL",
                    "EMAIL",
                    "Account creation email sent successfully to "
                            + savedUser.getEmail(),
                    "SUCCESS"
            );

        } catch (Exception e) {

            auditLogger.log(
                    savedUser.getUsername(),
                    "SEND ACCOUNT EMAIL",
                    "EMAIL",
                    "Account was created successfully but "
                            + "registration email could not be sent.",
                    "FAILED"
            );
        }

        notificationService.notify(
                savedUser,
                "Your IRIFAMS account has been created successfully."
        );

        return ResponseEntity.ok(
                "User registered successfully"
        );
    }


    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository
                    .findByUsername(request.getUsername())
                    .orElseThrow();

            String token =
                    jwtService.generateToken(
                            user.getUsername()
                    );

            auditLogger.log(
                    user.getUsername(),
                    "LOGIN",
                    "AUTH",
                    "User logged into the system",
                    "SUCCESS"
            );

            return ResponseEntity.ok(

                    AuthResponse.builder()
                            .id(user.getId())
                            .token(token)
                            .username(user.getUsername())
                            .fullName(user.getFullName())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .gender(user.getGender())
                            .institution(user.getInstitution())
                            .blockName(user.getBlockName())
                            .image(user.getImage())
                            .role(user.getRole().name())
                            .temporaryPassword(
                                    user.isTemporaryPassword()
                            )
                            .build()
            );

        } catch (Exception e) {

            auditLogger.log(
                    request.getUsername(),
                    "LOGIN",
                    "AUTH",
                    "Invalid login attempt",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body(
                            "Invalid username or password"
                    );
        }
    }


    // =========================================================
// FORGOT PASSWORD
// SEND OTP
// FARMER ONLY
// =========================================================

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        // -----------------------------------------------------
        // VALIDATE USERNAME
        // -----------------------------------------------------

        if (request.getUsername() == null
                || request.getUsername().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Username is required.");
        }

        String username =
                request.getUsername().trim();


        // -----------------------------------------------------
        // FIND USER
        // -----------------------------------------------------

        User user = userRepository
                .findByUsername(username)
                .orElse(null);


        // -----------------------------------------------------
        // USER NOT FOUND
        // -----------------------------------------------------

        if (user == null) {

            auditLogger.log(
                    username,
                    "FORGOT PASSWORD",
                    "AUTH",
                    "Password recovery failed. Username not found.",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body(
                            "No account was found with this username."
                    );
        }


        // -----------------------------------------------------
        // FARMER ONLY
        // -----------------------------------------------------

        if (user.getRole() != Role.FARMER) {

            auditLogger.log(
                    user.getUsername(),
                    "FORGOT PASSWORD",
                    "AUTH",
                    "Password recovery denied. "
                            + "Password recovery is available only for Farmers.",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body(
                            "Password recovery is available only for Farmers."
                    );
        }


        // -----------------------------------------------------
        // ACCOUNT MUST BE ENABLED
        // -----------------------------------------------------

        if (!user.isEnabled()) {

            auditLogger.log(
                    user.getUsername(),
                    "FORGOT PASSWORD",
                    "AUTH",
                    "Password recovery denied. Account is disabled.",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body(
                            "Your account is currently disabled. Please contact the administrator."
                    );
        }


        // -----------------------------------------------------
        // EMAIL REQUIRED
        // -----------------------------------------------------

        if (user.getEmail() == null
                || user.getEmail().isBlank()) {

            auditLogger.log(
                    user.getUsername(),
                    "FORGOT PASSWORD",
                    "AUTH",
                    "Password recovery failed. User email is not available.",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body(
                            "No email address is registered for this account."
                    );
        }


        // -----------------------------------------------------
        // DELETE PREVIOUS OTP
        // -----------------------------------------------------

        otpRepository.deleteByUserId(
                user.getId()
        );


        // -----------------------------------------------------
        // GENERATE NEW OTP
        // -----------------------------------------------------

        String otp =
                OtpGenerator.generate();


        LocalDateTime now =
                LocalDateTime.now();


        // -----------------------------------------------------
        // CREATE OTP RECORD
        // -----------------------------------------------------

        OtpVerification verification =
                OtpVerification.builder()
                        .user(user)
                        .otp(otp)
                        .expiresAt(
                                now.plusMinutes(5)
                        )
                        .verified(false)
                        .attempts(0)
                        .recoveryToken(null)
                        .createdAt(now)
                        .build();


        otpRepository.save(
                verification
        );


        // -----------------------------------------------------
        // SEND OTP EMAIL
        // -----------------------------------------------------

        try {

            emailService.sendOtpEmail(
                    user.getEmail(),
                    user.getFullName(),
                    otp
            );

            auditLogger.log(
                    user.getUsername(),
                    "SEND OTP EMAIL",
                    "EMAIL",
                    "OTP sent successfully to "
                            + user.getEmail(),
                    "SUCCESS"
            );

        } catch (Exception e) {

            otpRepository.delete(
                    verification
            );

            auditLogger.log(
                    user.getUsername(),
                    "SEND OTP EMAIL",
                    "EMAIL",
                    "OTP email could not be sent.",
                    "FAILED"
            );

            return ResponseEntity.internalServerError()
                    .body(
                            "Unable to send OTP. Please try again later."
                    );
        }


        // -----------------------------------------------------
        // NOTIFICATION
        // -----------------------------------------------------

        notificationService.notify(
                user,
                "A password recovery OTP has been sent to your registered email."
        );


        // -----------------------------------------------------
        // AUDIT
        // -----------------------------------------------------

        auditLogger.log(
                user.getUsername(),
                "FORGOT PASSWORD",
                "AUTH",
                "Password recovery OTP generated and sent successfully.",
                "SUCCESS"
        );


        // -----------------------------------------------------
        // RESPONSE
        // -----------------------------------------------------

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "OTP has been sent to your registered email address."
                )
        );
    }

    // =========================================================
    // VERIFY OTP
    // =========================================================

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody VerifyOtpRequest request) {

        // -----------------------------------------------------
        // VALIDATE REQUEST
        // -----------------------------------------------------

        if (request.getUsername() == null
                || request.getUsername().isBlank()
                || request.getOtp() == null
                || request.getOtp().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(
                            "Username and OTP are required."
                    );
        }


        String username =
                request.getUsername().trim();

        String enteredOtp =
                request.getOtp().trim();


        // -----------------------------------------------------
        // FIND USER
        // -----------------------------------------------------

        User user = userRepository
                .findByUsername(username)
                .orElse(null);


        if (user == null) {

            return ResponseEntity.badRequest()
                    .body(
                            "Invalid OTP verification request."
                    );
        }


        // -----------------------------------------------------
        // FIND LATEST OTP
        // -----------------------------------------------------

        OtpVerification verification =
                otpRepository
                        .findTopByUserUsernameOrderByCreatedAtDesc(
                                user.getUsername()
                        )
                        .orElse(null);


        if (verification == null) {

            auditLogger.log(
                    user.getUsername(),
                    "VERIFY OTP",
                    "AUTH",
                    "No OTP verification record found.",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body(
                            "OTP not found. Please request a new OTP."
                    );
        }


        // -----------------------------------------------------
        // ALREADY VERIFIED
        // -----------------------------------------------------

        if (verification.isVerified()) {

            return ResponseEntity.badRequest()
                    .body(
                            "This OTP has already been verified."
                    );
        }


        // -----------------------------------------------------
        // CHECK EXPIRATION
        // -----------------------------------------------------

        if (LocalDateTime.now()
                .isAfter(
                        verification.getExpiresAt()
                )) {

            auditLogger.log(
                    user.getUsername(),
                    "VERIFY OTP",
                    "AUTH",
                    "OTP verification failed. OTP expired.",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body(
                            "OTP has expired. Please request a new OTP."
                    );
        }


        // -----------------------------------------------------
        // CHECK MAXIMUM ATTEMPTS
        // -----------------------------------------------------

        if (verification.getAttempts() >= 5) {

            return ResponseEntity.badRequest()
                    .body(
                            "Too many incorrect attempts. Please request a new OTP."
                    );
        }


        // -----------------------------------------------------
        // VERIFY OTP
        // -----------------------------------------------------

        if (!verification.getOtp()
                .equals(enteredOtp)) {

            verification.setAttempts(
                    verification.getAttempts() + 1
            );

            otpRepository.save(
                    verification
            );

            auditLogger.log(
                    user.getUsername(),
                    "VERIFY OTP",
                    "AUTH",
                    "Incorrect OTP entered.",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body(
                            "Invalid OTP."
                    );
        }


        // -----------------------------------------------------
        // OTP VALID
        // -----------------------------------------------------

        String recoveryToken =
                UUID.randomUUID().toString();


        verification.setVerified(true);

        verification.setRecoveryToken(
                recoveryToken
        );


        otpRepository.save(
                verification
        );


        auditLogger.log(
                user.getUsername(),
                "VERIFY OTP",
                "AUTH",
                "OTP verified successfully.",
                "SUCCESS"
        );


        return ResponseEntity.ok(

                OtpResponse.builder()
                        .message(
                                "OTP verified successfully."
                        )
                        .recoveryToken(
                                recoveryToken
                        )
                        .build()
        );
    }


    // =========================================================
    // RESET PASSWORD AFTER OTP
    // =========================================================

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        // -----------------------------------------------------
        // VALIDATE REQUEST
        // -----------------------------------------------------

        if (request.getUsername() == null
                || request.getUsername().isBlank()
                || request.getRecoveryToken() == null
                || request.getRecoveryToken().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(
                            "Invalid password reset request."
                    );
        }


        // -----------------------------------------------------
        // FIND USER
        // -----------------------------------------------------

        User user = userRepository
                .findByUsername(
                        request.getUsername().trim()
                )
                .orElse(null);


        if (user == null) {

            return ResponseEntity.badRequest()
                    .body(
                            "Invalid password reset request."
                    );
        }


        // -----------------------------------------------------
        // VALIDATE PASSWORD
        // -----------------------------------------------------

        if (request.getNewPassword() == null
                || request.getNewPassword().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(
                            "New password is required."
                    );
        }


        if (request.getNewPassword().length() < 4) {

            return ResponseEntity.badRequest()
                    .body(
                            "Password must contain at least 4 characters."
                    );
        }


        // -----------------------------------------------------
        // FIND OTP SESSION
        // -----------------------------------------------------

        OtpVerification verification =
                otpRepository
                        .findTopByUserUsernameOrderByCreatedAtDesc(
                                user.getUsername()
                        )
                        .orElse(null);


        if (verification == null) {

            return ResponseEntity.badRequest()
                    .body(
                            "Password recovery session not found."
                    );
        }


        // -----------------------------------------------------
        // CHECK OTP EXPIRATION AGAIN
        // -----------------------------------------------------

        if (LocalDateTime.now()
                .isAfter(
                        verification.getExpiresAt()
                )) {

            auditLogger.log(
                    user.getUsername(),
                    "RESET PASSWORD",
                    "AUTH",
                    "Password reset failed. Recovery session expired.",
                    "FAILED"
            );

            return ResponseEntity.status(403)
                    .body(
                            "Password recovery session has expired. Please request a new OTP."
                    );
        }


        // -----------------------------------------------------
        // VERIFY RECOVERY TOKEN
        // -----------------------------------------------------

        if (!verification.isVerified()
                || verification.getRecoveryToken() == null
                || !verification.getRecoveryToken()
                .equals(
                        request.getRecoveryToken().trim()
                )) {

            auditLogger.log(
                    user.getUsername(),
                    "RESET PASSWORD",
                    "AUTH",
                    "Invalid password recovery token.",
                    "FAILED"
            );

            return ResponseEntity.status(403)
                    .body(
                            "Invalid or expired password recovery session."
                    );
        }


        // -----------------------------------------------------
        // RESET PASSWORD
        // -----------------------------------------------------

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        user.setTemporaryPassword(false);

        userRepository.save(user);


        // -----------------------------------------------------
        // DELETE USED OTP
        // -----------------------------------------------------

        otpRepository.delete(
                verification
        );


        // -----------------------------------------------------
        // AUDIT
        // -----------------------------------------------------

        auditLogger.log(
                user.getUsername(),
                "RESET PASSWORD",
                "AUTH",
                "Password was reset successfully using OTP verification.",
                "SUCCESS"
        );


        // -----------------------------------------------------
        // NOTIFICATION
        // -----------------------------------------------------

        notificationService.notify(
                user,
                "Your password has been reset successfully."
        );


        // -----------------------------------------------------
        // CONFIRMATION EMAIL
        // -----------------------------------------------------

        try {

            emailService.sendPasswordChangedEmail(
                    user
            );

        } catch (Exception e) {

            auditLogger.log(
                    user.getUsername(),
                    "SEND PASSWORD EMAIL",
                    "EMAIL",
                    "Password was reset successfully but confirmation email could not be sent.",
                    "FAILED"
            );
        }


        return ResponseEntity.ok(
                "Password reset successfully."
        );
    }
}