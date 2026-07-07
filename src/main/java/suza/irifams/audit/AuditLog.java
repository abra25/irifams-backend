package suza.irifams.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String action;

    private String module;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String status; // SUCCESS / FAILED

    private LocalDateTime createdAt;
}