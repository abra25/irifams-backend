package suza.irifams.input;

import jakarta.persistence.*;
import lombok.*;
import suza.irifams.enums.InputCategory;

@Entity
@Table(name = "farm_inputs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmInput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private InputCategory category;

    private Double quantity;

    private String unit;

    private String season;

    private String image;

    private String status;

}