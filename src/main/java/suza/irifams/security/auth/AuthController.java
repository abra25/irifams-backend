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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogger auditLogger;

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
                .enabled(true)

                .build();

        userRepository.save(user);

        User savedUser = userRepository.save(user);

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
                    .body("Invalid username or password");
        }
    }
}