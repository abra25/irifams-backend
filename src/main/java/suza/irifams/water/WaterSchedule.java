package suza.irifams.water;

import jakarta.persistence.*;
import lombok.*;
import suza.irifams.enums.ScheduleStatus;
import suza.irifams.plot.Plot;
import suza.irifams.user.User;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "water_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaterSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate irrigationDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String canal;

    private String season;

    @Enumerated(EnumType.STRING)
    private ScheduleStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne
    @JoinColumn(name = "plot_id")
    private Plot plot;

    @ManyToOne
    @JoinColumn(name = "supervisor_id")
    private User supervisor;

    private boolean reminderSent;

    private boolean startedNotificationSent;

    private boolean completedNotificationSent;



}