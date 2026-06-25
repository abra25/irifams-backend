package suza.irifams.request;

import jakarta.persistence.*;
import lombok.*;
import suza.irifams.enums.RequestStatus;
import suza.irifams.enums.ServiceType;
import suza.irifams.plot.Plot;
import suza.irifams.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate preferredDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Double amount;

    private String controlNumber;

    private boolean paymentConfirmed;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    // Farmer

    @ManyToOne
    @JoinColumn(name = "farmer_id")
    private User farmer;

    // Plot

    @ManyToOne
    @JoinColumn(name = "plot_id")
    private Plot plot;

}