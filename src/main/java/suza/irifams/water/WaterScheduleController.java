package suza.irifams.water;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.enums.ScheduleStatus;
import suza.irifams.notification.Notification;
import suza.irifams.notification.NotificationRepository;
import suza.irifams.plot.Plot;
import suza.irifams.plot.PlotRepository;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;
import org.springframework.security.core.Authentication;

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
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<?> createSchedule(

            @RequestParam Long plotId,

            @RequestBody WaterSchedule schedule,

            Authentication authentication

    ) {

        String username =
                authentication.getName();

        User supervisor = userRepository

                .findByUsername(username)

                .orElse(null);

        if (supervisor == null) {

            auditLogger.log(
                    "UNKNOWN",
                    "CREATE WATER SCHEDULE",
                    "IRRIGATION",
                    "Failed. Supervisor not found",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body("Supervisor not found");
        }

        Plot plot = plotRepository

                .findById(plotId)

                .orElse(null);

        if (plot == null) {

            auditLogger.log(
                    supervisor.getUsername(),
                    "CREATE WATER SCHEDULE",
                    "IRRIGATION",
                    "Failed. Plot not found",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body("Plot not found");
        }

        // Supervisor aruhusiwe kupanga
        // schedule za block yake tu

        if (supervisor.getRole().name()
                .equals("SUPERVISOR")) {

            if (!plot.getBlock()
                    .equalsIgnoreCase(
                            supervisor.getBlockName())) {

                auditLogger.log(
                        supervisor.getUsername(),
                        "CREATE WATER SCHEDULE",
                        "IRRIGATION",
                        "Attempted to create schedule outside assigned block",
                        "FAILED"
                );

                return ResponseEntity.badRequest()
                        .body(
                                "You can only manage schedules within your block"
                        );
            }
        }

        schedule.setPlot(plot);

        schedule.setSupervisor(supervisor);

        if(schedule.getStatus() == null){
            schedule.setStatus(ScheduleStatus.UPCOMING);
        }

        WaterSchedule saved =
                repository.save(schedule);

        auditLogger.log(
                supervisor.getUsername(),
                "CREATE WATER SCHEDULE",
                "IRRIGATION",
                "Created irrigation schedule for Plot "
                        + plot.getPlotNo(),
                "SUCCESS"
        );

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

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/my-farm-schedules")
    public List<WaterSchedule> getMyFarmSchedules(
            Authentication authentication){

        String username = authentication.getName();

        User farmer = userRepository
                .findByUsername(username)
                .orElseThrow();

        return repository.findByPlot_Farmer_IdOrderByIrrigationDateAsc(
                farmer.getId()
        );
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/my-plots")
    public List<Plot> getMyPlots(
            Authentication authentication
    ) {

        String username =
                authentication.getName();

        User supervisor = userRepository
                .findByUsername(username)
                .orElseThrow();

        return plotRepository.findByBlock(
                supervisor.getBlockName()
        );
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

                    auditLogger.log(
                            schedule.getPlot()
                                    .getFarmer()
                                    .getUsername(),

                            "UPDATE WATER SCHEDULE",

                            "IRRIGATION",

                            "Water schedule updated successfully",

                            "SUCCESS"
                    );

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
    ){

        WaterSchedule schedule =
                repository.findById(id)
                        .orElse(null);

        if(schedule == null){

            auditLogger.log(
                    "UNKNOWN",
                    "DELETE WATER SCHEDULE",
                    "IRRIGATION",
                    "Failed to delete schedule. Schedule not found",
                    "FAILED"
            );

            return ResponseEntity.notFound().build();
        }

        auditLogger.log(
                schedule.getPlot()
                        .getFarmer()
                        .getUsername(),

                "DELETE WATER SCHEDULE",

                "IRRIGATION",

                "Water schedule deleted successfully",

                "SUCCESS"
        );

        repository.deleteById(id);

        return ResponseEntity.ok(
                "Schedule deleted successfully"
        );
    }

    /*
     * Supervisor schedules by own block
     */

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/my-schedules")
    public List<WaterSchedule> getMySchedules(

            Authentication authentication

    ) {

        String username =
                authentication.getName();

        User supervisor = userRepository

                .findByUsername(username)

                .orElseThrow();

        return repository.findByPlot_Block(

                supervisor.getBlockName()

        );

    }
}