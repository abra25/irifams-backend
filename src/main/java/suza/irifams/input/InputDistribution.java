package suza.irifams.input;

import jakarta.persistence.*;
import lombok.*;
import suza.irifams.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "input_distributions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InputDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double quantity;

    private LocalDateTime distributedAt;

    private String season;

    @ManyToOne
    @JoinColumn(name = "input_id")
    private FarmInput farmInput;

    @ManyToOne
    @JoinColumn(name = "farmer_id")
    private User farmer;

}