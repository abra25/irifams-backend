package suza.irifams.plot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import suza.irifams.user.User;

@Entity
@Table(name = "plots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String plotNo;

    private String block;

    private Double size;

    private String soilType;

    private String locationDescription;

    private String irrigationMethod;

    private String season;

    private String status;

    // Owner (Farmer)

    @ManyToOne
    @JoinColumn(name = "farmer_id")
    @JsonIgnoreProperties({"password"})
    private User farmer;

}