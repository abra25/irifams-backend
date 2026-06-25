package suza.irifams.payment;

import jakarta.persistence.*;
import lombok.*;
import suza.irifams.enums.PaymentStatus;
import suza.irifams.request.ServiceRequest;
import suza.irifams.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String controlNumber;

    private Double amount;

    private String receiptNumber;

    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @ManyToOne
    @JoinColumn(name = "farmer_id")
    private User farmer;

    @OneToOne
    @JoinColumn(name = "request_id")
    private ServiceRequest serviceRequest;
}