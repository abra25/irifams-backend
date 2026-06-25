package suza.irifams.water;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.notification.Notification;
import suza.irifams.notification.NotificationRepository;
import suza.irifams.plot.Plot;
import suza.irifams.plot.PlotRepository;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/water-schedules")
@RequiredArgsConstructor
public class WaterScheduleController {

    private final WaterScheduleRepository repository;
    private final PlotRepository plotRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogger auditLogger;

    /*
     * Get all schedules
     */

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR','STAKEHOLDER')")
    public List<WaterSchedule> getAllSchedules() {

        return repository.findAll();

    }

    /*
     * Farmer schedules by plot
     */

    @GetMapping("/plot/{plotId}")
    @PreAuthorize(
            "hasAnyRole('FARMER','ADMIN','SUPERVISOR')")
    public List<WaterSchedule> getPlotSchedules(
            @PathVariable Long plotId
    ) {

        return repository.findByPlotId(plotId);

    }

    /*
     * Create schedule
     */

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<?> createSchedule(

            @RequestParam Long plotId,

            @RequestParam Long supervisorId,

            @RequestBody WaterSchedule schedule

    ) {

        Plot plot = plotRepository.findById(plotId)
                .orElseThrow();

        User supervisor = userRepository.findById(supervisorId)
                .orElseThrow();

        schedule.setPlot(plot);
        schedule.setSupervisor(supervisor);

        WaterSchedule saved =
                repository.save(schedule);

        auditLogger.log(
                plot.getFarmer().getUsername(),
                "WATER_SCHEDULE",
                "IRRIGATION",
                "New irrigation schedule assigned"
        );

        /*
         * Notify farmer
         */

        notificationRepository.save(

                Notification.builder()
                        .message(
                                "New irrigation schedule has been assigned for Plot "
                                        + plot.getPlotNo()
                        )
                        .user(plot.getFarmer())
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build()

        );

        return ResponseEntity.ok(saved);

    }

    /*
     * Update schedule
     */

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<?> updateSchedule(

            @PathVariable Long id,

            @RequestBody WaterSchedule updated

    ) {

        return repository.findById(id)

                .map(schedule -> {

                    schedule.setIrrigationDate(
                            updated.getIrrigationDate());

                    schedule.setStartTime(
                            updated.getStartTime());

                    schedule.setEndTime(
                            updated.getEndTime());

                    schedule.setCanal(
                            updated.getCanal());

                    schedule.setSeason(
                            updated.getSeason());

                    schedule.setStatus(
                            updated.getStatus());

                    schedule.setNotes(
                            updated.getNotes());

                    return ResponseEntity.ok(
                            repository.save(schedule)
                    );

                })

                .orElse(
                        ResponseEntity.notFound().build()
                );

    }

    /*
     * Delete schedule
     */

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSchedule(
            @PathVariable Long id
    ) {

        repository.deleteById(id);

        return ResponseEntity.ok(
                "Schedule deleted successfully"
        );

    }

}