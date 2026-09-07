package suza.irifams.security.auth;

import jakarta.persistence.*;
import lombok.*;
import suza.irifams.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String otp;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private boolean verified;

    private int attempts;

    @Column(unique = true)
    private String recoveryToken;

    private LocalDateTime createdAt;
}