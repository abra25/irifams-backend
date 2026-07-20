package suza.irifams.security.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import suza.irifams.audit.AuditLogger;
import suza.irifams.security.service.JwtService;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;
import suza.irifams.enums.Role;
import suza.irifams.notification.NotificationService;
import suza.irifams.security.auth.ForgotPasswordRequest;
import suza.irifams.security.auth.ForgotPasswordResponse;
import suza.irifams.security.util.PasswordGenerator;

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

    // REGISTER

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        if (userRepository.existsByUsername(
                request.getUsername())) {

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
                .password(passwordEncoder.encode(
                        request.getPassword()))
                .gender(request.getGender())
                .blockName(request.getBlockName())
                .institution(request.getInstitution())
                .role(request.getRole())
                .image(request.getImage())
                .temporaryPassword(false)
                .enabled(true)

                .build();

        User savedUser =
                userRepository.save(user);

        auditLogger.log(
                savedUser.getUsername(),
                "REGISTER",
                "USER",
                "New user account registered",
                "SUCCESS"
        );

        return ResponseEntity.ok(
                "User registered successfully");
    }

    // LOGIN

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        try {
            User dbUser = userRepository
                    .findByUsername(request.getUsername())
                    .orElse(null);

            System.out.println("======================");
            System.out.println("USERNAME = " + request.getUsername());

            if(dbUser != null){

                System.out.println("FOUND USER");

                System.out.println(
                        "PASSWORD ENTERED = "
                                + request.getPassword()
                );

                System.out.println(
                        "PASSWORD IN DB = "
                                + dbUser.getPassword()
                );

                System.out.println(
                        "MATCH = "
                                + passwordEncoder.matches(
                                request.getPassword(),
                                dbUser.getPassword()
                        )
                );

            }else{

                System.out.println("USER NOT FOUND");

            }

            System.out.println("======================");

            authenticationManager.authenticate(

                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository
                    .findByUsername(
                            request.getUsername())
                    .orElseThrow();

            String token =
                    jwtService.generateToken(
                            user.getUsername());

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

        } catch (Exception e){

            e.printStackTrace();

            auditLogger.log(
                    request.getUsername(),
                    "LOGIN",
                    "AUTH",
                    e.getMessage(),
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(

            @RequestBody ForgotPasswordRequest request

    ){

        User user = userRepository

                .findByUsername(request.getUsername())

                .orElse(null);

        // Username doesn't exist

        if(user == null){

            auditLogger.log(

                    request.getUsername(),

                    "FORGOT PASSWORD",

                    "AUTH",

                    "Password recovery failed. Username not found.",

                    "FAILED"

            );

            return ResponseEntity.badRequest()

                    .body("Invalid account information.");

        }

        // Verify full name

        if(!user.getFullName().trim()

                .equalsIgnoreCase(

                        request.getFullName().trim()

                )){

            auditLogger.log(

                    user.getUsername(),

                    "FORGOT PASSWORD",

                    "AUTH",

                    "Password recovery failed. Full name mismatch.",

                    "FAILED"

            );

            return ResponseEntity.badRequest()

                    .body("Invalid account information.");

        }

        // Verify phone

        if(!user.getPhone().trim()

                .equals(

                        request.getPhone().trim()

                )){

            auditLogger.log(

                    user.getUsername(),

                    "FORGOT PASSWORD",

                    "AUTH",

                    "Password recovery failed. Phone mismatch.",

                    "FAILED"

            );

            return ResponseEntity.badRequest()

                    .body("Invalid account information.");

        }

        // Only Farmer & Stakeholder

        if(

                user.getRole()!= Role.FARMER

                        &&

                        user.getRole()!= Role.STAKEHOLDER

        ){

            auditLogger.log(

                    user.getUsername(),

                    "FORGOT PASSWORD",

                    "AUTH",

                    "Password recovery denied. Unsupported role.",

                    "FAILED"

            );

            return ResponseEntity.badRequest()

                    .body(

                            "Password recovery is available only for Farmers and Stakeholders."

                    );

        }

        // Generate new password

        String tempPassword =

                PasswordGenerator.generate();

        user.setPassword(

                passwordEncoder.encode(

                        tempPassword

                )

        );

        user.setTemporaryPassword(true);

        userRepository.save(user);

        System.out.println("NEW PASSWORD : " + tempPassword);

        User test = userRepository
                .findByUsername(user.getUsername())
                .orElseThrow();

        System.out.println(
                passwordEncoder.matches(
                        tempPassword,
                        test.getPassword()
                )
        );

        auditLogger.log(

                user.getUsername(),

                "FORGOT PASSWORD",

                "AUTH",

                "Temporary password generated successfully.",

                "SUCCESS"

        );

        notificationService.notify(

                user,

                "Your password has been reset successfully. Please login using the temporary password and change it immediately."

        );

        return ResponseEntity.ok(

                ForgotPasswordResponse

                        .builder()

                        .message(

                                "Password reset successfully."

                        )

                        .temporaryPassword(

                                tempPassword

                        )

                        .build()

        );

    }
}