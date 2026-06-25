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
                "New user account registered"
        );

        return ResponseEntity.ok(
                "User registered successfully");
    }

    // LOGIN

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request) {

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
                        user.getUsername());
        auditLogger.log(
                user.getUsername(),
                "LOGIN",
                "AUTH",
                "User logged into the system"
        );

        return ResponseEntity.ok(

                AuthResponse.builder()
                        .token(token)
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .image(user.getImage())
                        .build()
        );
    }
}