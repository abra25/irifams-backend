package suza.irifams.water;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import suza.irifams.audit.AuditLogger;
import suza.irifams.email.EmailService;
import suza.irifams.enums.Role;
import suza.irifams.enums.ScheduleStatus;
import suza.irifams.notification.NotificationService;
import suza.irifams.plot.Plot;
import suza.irifams.plot.PlotRepository;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/water-schedules")
@RequiredArgsConstructor
public class WaterScheduleController {

    private final WaterScheduleRepository repository;
    private final PlotRepository plotRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;
    private final EmailService emailService;


    /*
     * =========================================================
     * GET ALL SCHEDULES
     * =========================================================
     */

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<WaterSchedule> getAllSchedules() {

        return repository.findAll();
    }


    /*
     * =========================================================
     * FARMER SCHEDULES BY PLOT
     * =========================================================
     */

    @GetMapping("/plot/{plotId}")
    @PreAuthorize(
            "hasAnyRole('FARMER','ADMIN','SUPERVISOR')"
    )
    public List<WaterSchedule> getPlotSchedules(

            @PathVariable Long plotId

    ) {

        return repository.findByPlotId(plotId);
    }


    /*
     * =========================================================
     * CREATE SCHEDULE
     * =========================================================
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


        User supervisor =
                userRepository
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


        Plot plot =
                plotRepository
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


        /*
         * =====================================================
         * SUPERVISOR BLOCK RESTRICTION
         * =====================================================
         */

        if (
                supervisor.getRole()
                        == Role.SUPERVISOR
        ) {

            if (
                    plot.getBlock() == null
                            ||
                            supervisor.getBlockName() == null
                            ||
                            !plot.getBlock()
                                    .equalsIgnoreCase(
                                            supervisor.getBlockName()
                                    )
            ) {

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


        /*
         * =====================================================
         * ASSIGN RELATIONSHIPS
         * =====================================================
         */

        schedule.setPlot(plot);

        schedule.setSupervisor(supervisor);


        if (schedule.getStatus() == null) {

            schedule.setStatus(
                    ScheduleStatus.UPCOMING
            );
        }


        /*
         * =====================================================
         * SAVE
         * =====================================================
         */

        WaterSchedule saved =
                repository.save(schedule);


        /*
         * =====================================================
         * AUDIT
         * =====================================================
         */

        auditLogger.log(

                supervisor.getUsername(),

                "CREATE WATER SCHEDULE",

                "IRRIGATION",

                "Created irrigation schedule for Plot "
                        + plot.getPlotNo(),

                "SUCCESS"
        );


        /*
         * =====================================================
         * NOTIFICATION TO FARMER
         * =====================================================
         */

        notificationService.notify(

                plot.getFarmer(),

                "New irrigation schedule has been assigned for Plot "
                        + plot.getPlotNo()
        );


        /*
         * =====================================================
         * NOTIFICATION TO SUPERVISOR
         * =====================================================
         */

        notificationService.notify(

                supervisor,

                "You created a new irrigation schedule for Plot "
                        + plot.getPlotNo()
        );


        /*
         * =====================================================
         * NOTIFICATION TO OTHER ADMINS
         * =====================================================
         */

        userRepository.findByRole(Role.ADMIN)

                .stream()

                .filter(
                        admin ->
                                !admin.getId()
                                        .equals(
                                                supervisor.getId()
                                        )
                )

                .forEach(

                        admin ->

                                notificationService.notify(

                                        admin,

                                        "New irrigation schedule created for Plot "
                                                + plot.getPlotNo()
                                )
                );


        /*
         * =====================================================
         * GMAIL - FARMER
         * =====================================================
         */

        try {

            emailService.sendWaterScheduleEmail(

                    plot.getFarmer(),

                    saved,

                    "CREATED"
            );


            auditLogger.log(

                    supervisor.getUsername(),

                    "SEND WATER SCHEDULE EMAIL",

                    "EMAIL",

                    "Water schedule creation email sent successfully to "
                            + plot.getFarmer().getEmail(),

                    "SUCCESS"
            );

        } catch (Exception e) {

            /*
             * Email failure must NOT cancel
             * the successful schedule creation.
             */

            auditLogger.log(

                    supervisor.getUsername(),

                    "SEND WATER SCHEDULE EMAIL",

                    "EMAIL",

                    "Water schedule was created successfully but "
                            + "email could not be sent.",

                    "FAILED"
            );
        }


        return ResponseEntity.ok(saved);
    }


    /*
     * =========================================================
     * FARMER'S OWN SCHEDULES
     * =========================================================
     */

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/my-farm-schedules")
    public List<WaterSchedule> getMyFarmSchedules(

            Authentication authentication

    ) {

        String username =
                authentication.getName();


        User farmer =
                userRepository
                        .findByUsername(username)
                        .orElseThrow();


        return repository
                .findByPlot_Farmer_IdOrderByIrrigationDateAsc(
                        farmer.getId()
                );
    }


    /*
     * =========================================================
     * SUPERVISOR'S PLOTS
     * =========================================================
     */

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/my-plots")
    public List<Plot> getMyPlots(

            Authentication authentication

    ) {

        String username =
                authentication.getName();


        User supervisor =
                userRepository
                        .findByUsername(username)
                        .orElseThrow();


        return plotRepository.findByBlock(
                supervisor.getBlockName()
        );
    }


    /*
     * =========================================================
     * UPDATE SCHEDULE
     * =========================================================
     */

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<?> updateSchedule(

            @PathVariable Long id,

            @RequestBody WaterSchedule updated,

            Authentication authentication

    ) {

        return repository.findById(id)

                .map(schedule -> {

                    User actor =
                            userRepository

                                    .findByUsername(
                                            authentication.getName()
                                    )

                                    .orElseThrow();


                    /*
                     * -----------------------------------------
                     * SUPERVISOR BLOCK RESTRICTION
                     * -----------------------------------------
                     */

                    if (
                            actor.getRole()
                                    == Role.SUPERVISOR
                    ) {

                        if (
                                schedule.getPlot() == null
                                        ||
                                        schedule.getPlot().getBlock() == null
                                        ||
                                        actor.getBlockName() == null
                                        ||
                                        !schedule.getPlot()
                                                .getBlock()
                                                .equalsIgnoreCase(
                                                        actor.getBlockName()
                                                )
                        ) {

                            auditLogger.log(

                                    actor.getUsername(),

                                    "UPDATE WATER SCHEDULE",

                                    "IRRIGATION",

                                    "Attempted to update schedule outside assigned block",

                                    "FAILED"
                            );

                            return ResponseEntity
                                    .badRequest()
                                    .body(
                                            "You can only manage schedules within your block"
                                    );
                        }
                    }


                    /*
                     * -----------------------------------------
                     * UPDATE FIELDS
                     * -----------------------------------------
                     */

                    schedule.setIrrigationDate(
                            updated.getIrrigationDate()
                    );

                    schedule.setStartTime(
                            updated.getStartTime()
                    );

                    schedule.setEndTime(
                            updated.getEndTime()
                    );

                    schedule.setCanal(
                            updated.getCanal()
                    );

                    schedule.setSeason(
                            updated.getSeason()
                    );

                    schedule.setStatus(
                            updated.getStatus()
                    );

                    schedule.setNotes(
                            updated.getNotes()
                    );


                    repository.save(schedule);


                    /*
                     * -----------------------------------------
                     * AUDIT
                     * -----------------------------------------
                     */

                    auditLogger.log(

                            actor.getUsername(),

                            "UPDATE WATER SCHEDULE",

                            "IRRIGATION",

                            "Updated irrigation schedule #"
                                    + schedule.getId(),

                            "SUCCESS"
                    );


                    /*
                     * -----------------------------------------
                     * FARMER NOTIFICATION
                     * -----------------------------------------
                     */

                    notificationService.notify(

                            schedule.getPlot().getFarmer(),

                            "Your irrigation schedule has been updated."
                    );


                    /*
                     * -----------------------------------------
                     * ACTOR NOTIFICATION
                     * -----------------------------------------
                     */

                    notificationService.notify(

                            actor,

                            "You updated irrigation schedule #"
                                    + schedule.getId()
                    );


                    /*
                     * -----------------------------------------
                     * ADMIN NOTIFICATIONS
                     * -----------------------------------------
                     */

                    userRepository.findByRole(Role.ADMIN)

                            .stream()

                            .filter(
                                    admin ->
                                            !admin.getId()
                                                    .equals(
                                                            actor.getId()
                                                    )
                            )

                            .forEach(

                                    admin ->

                                            notificationService.notify(

                                                    admin,

                                                    "Water schedule #"
                                                            + schedule.getId()
                                                            + " has been updated."
                                            )
                            );


                    /*
                     * -----------------------------------------
                     * GMAIL - FARMER
                     * -----------------------------------------
                     */

                    try {

                        emailService.sendWaterScheduleEmail(

                                schedule.getPlot().getFarmer(),

                                schedule,

                                "UPDATED"
                        );


                        auditLogger.log(

                                actor.getUsername(),

                                "SEND WATER SCHEDULE EMAIL",

                                "EMAIL",

                                "Water schedule update email sent successfully to "
                                        + schedule.getPlot()
                                        .getFarmer()
                                        .getEmail(),

                                "SUCCESS"
                        );

                    } catch (Exception e) {

                        auditLogger.log(

                                actor.getUsername(),

                                "SEND WATER SCHEDULE EMAIL",

                                "EMAIL",

                                "Water schedule was updated successfully but "
                                        + "email could not be sent.",

                                "FAILED"
                        );
                    }


                    return ResponseEntity.ok(
                            schedule
                    );

                })

                .orElseGet(() -> {

                    auditLogger.log(

                            authentication.getName(),

                            "UPDATE WATER SCHEDULE",

                            "IRRIGATION",

                            "Schedule not found",

                            "FAILED"
                    );


                    return ResponseEntity.notFound()
                            .build();
                });
    }


    /*
     * =========================================================
     * DELETE SCHEDULE
     * =========================================================
     */

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSchedule(

            @PathVariable Long id,

            Authentication authentication

    ) {

        User actor =
                userRepository

                        .findByUsername(
                                authentication.getName()
                        )

                        .orElseThrow();


        WaterSchedule schedule =
                repository

                        .findById(id)

                        .orElse(null);


        if (schedule == null) {

            auditLogger.log(

                    actor.getUsername(),

                    "DELETE WATER SCHEDULE",

                    "IRRIGATION",

                    "Schedule not found",

                    "FAILED"
            );

            return ResponseEntity.notFound()
                    .build();
        }


        /*
         * Save farmer reference before deletion.
         */

        User farmer =
                schedule.getPlot() != null
                        ? schedule.getPlot().getFarmer()
                        : null;


        Long scheduleId =
                schedule.getId();


        repository.delete(schedule);


        /*
         * =====================================================
         * AUDIT
         * =====================================================
         */

        auditLogger.log(

                actor.getUsername(),

                "DELETE WATER SCHEDULE",

                "IRRIGATION",

                "Deleted schedule #"
                        + scheduleId,

                "SUCCESS"
        );


        /*
         * =====================================================
         * FARMER NOTIFICATION
         * =====================================================
         */

        notificationService.notify(

                farmer,

                "Your irrigation schedule has been deleted."
        );


        /*
         * =====================================================
         * ACTOR NOTIFICATION
         * =====================================================
         */

        notificationService.notify(

                actor,

                "You deleted irrigation schedule #"
                        + scheduleId
        );


        /*
         * =====================================================
         * GMAIL - FARMER
         * =====================================================
         *
         * Email is sent before the deleted entity is no longer
         * available. The farmer reference is already stored.
         */

        try {

            emailService.sendWaterScheduleEmail(

                    farmer,

                    schedule,

                    "DELETED"
            );


            auditLogger.log(

                    actor.getUsername(),

                    "SEND WATER SCHEDULE EMAIL",

                    "EMAIL",

                    "Water schedule deletion email sent successfully to "
                            + (
                            farmer != null
                                    ? farmer.getEmail()
                                    : "N/A"
                    ),

                    "SUCCESS"
            );

        } catch (Exception e) {

            auditLogger.log(

                    actor.getUsername(),

                    "SEND WATER SCHEDULE EMAIL",

                    "EMAIL",

                    "Water schedule was deleted successfully but "
                            + "email could not be sent.",

                    "FAILED"
            );
        }


        return ResponseEntity.ok(

                "Schedule deleted successfully"
        );
    }


    /*
     * =========================================================
     * SUPERVISOR SCHEDULES BY OWN BLOCK
     * =========================================================
     */

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/my-schedules")
    public List<WaterSchedule> getMySchedules(

            Authentication authentication

    ) {

        String username =
                authentication.getName();


        User supervisor =
                userRepository

                        .findByUsername(username)

                        .orElseThrow();


        return repository.findByPlot_Block(

                supervisor.getBlockName()
        );
    }

}