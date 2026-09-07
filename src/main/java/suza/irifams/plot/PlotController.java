package suza.irifams.plot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.email.EmailService;
import suza.irifams.enums.Role;
import suza.irifams.notification.NotificationService;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/plots")
@RequiredArgsConstructor
public class PlotController {

    private final PlotRepository plotRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;
    private final EmailService emailService;


    /*
     * =========================================================
     * GET ALL PLOTS
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @GetMapping
    public List<Plot> getAllPlots() {

        return plotRepository.findAll();
    }


    /*
     * =========================================================
     * GET PLOT BY ID
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','FARMER')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getPlot(
            @PathVariable Long id
    ) {

        return plotRepository.findById(id)

                .map(ResponseEntity::ok)

                .orElse(
                        ResponseEntity.notFound().build()
                );
    }


    /*
     * =========================================================
     * CREATE NEW PLOT
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PostMapping
    public ResponseEntity<?> addPlot(

            @RequestParam Long farmerId,

            @RequestBody Plot plot,

            Authentication authentication

    ) {

        User actor =
                userRepository

                        .findByUsername(
                                authentication.getName()
                        )

                        .orElseThrow();


        /*
         * -----------------------------------------------------
         * CHECK PLOT NUMBER
         * -----------------------------------------------------
         */

        if (
                plotRepository.existsByPlotNo(
                        plot.getPlotNo()
                )
        ) {

            auditLogger.log(

                    actor.getUsername(),

                    "CREATE PLOT",

                    "PLOT",

                    "Plot number already exists",

                    "FAILED"
            );


            return ResponseEntity.badRequest()

                    .body(
                            "Plot Number already exists"
                    );
        }


        /*
         * -----------------------------------------------------
         * FIND FARMER
         * -----------------------------------------------------
         */

        User farmer =
                userRepository

                        .findById(farmerId)

                        .orElse(null);


        if (farmer == null) {

            auditLogger.log(

                    actor.getUsername(),

                    "CREATE PLOT",

                    "PLOT",

                    "Farmer not found",

                    "FAILED"
            );


            return ResponseEntity.badRequest()

                    .body(
                            "Farmer not found"
                    );
        }


        /*
         * -----------------------------------------------------
         * SET BLOCK
         * -----------------------------------------------------
         */

        plot.setBlock(
                actor.getBlockName()
        );


        plot.setFarmer(
                farmer
        );


        /*
         * -----------------------------------------------------
         * SAVE PLOT
         * -----------------------------------------------------
         */

        Plot savedPlot =
                plotRepository.save(plot);


        /*
         * -----------------------------------------------------
         * AUDIT
         * -----------------------------------------------------
         */

        auditLogger.log(

                actor.getUsername(),

                "CREATE PLOT",

                "PLOT",

                "Created plot "
                        + savedPlot.getPlotNo(),

                "SUCCESS"
        );


        /*
         * -----------------------------------------------------
         * FARMER NOTIFICATION
         * -----------------------------------------------------
         */

        notificationService.notify(

                farmer,

                "A new plot ("
                        + savedPlot.getPlotNo()
                        + ") has been assigned to your account."
        );


        /*
         * -----------------------------------------------------
         * ACTOR NOTIFICATION
         * -----------------------------------------------------
         */

        notificationService.notify(

                actor,

                "You successfully created Plot "
                        + savedPlot.getPlotNo()
        );


        /*
         * -----------------------------------------------------
         * NOTIFY OTHER ADMINS
         * -----------------------------------------------------
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

                                        actor.getFullName()
                                                + " created Plot "
                                                + savedPlot.getPlotNo()
                                )
                );


        /*
         * =====================================================
         * GMAIL - FARMER
         * =====================================================
         */

        try {

            emailService.sendPlotEmail(

                    farmer,

                    savedPlot,

                    "CREATED"
            );


            auditLogger.log(

                    actor.getUsername(),

                    "SEND PLOT EMAIL",

                    "EMAIL",

                    "Plot creation email sent successfully to "
                            + farmer.getEmail(),

                    "SUCCESS"
            );

        } catch (Exception e) {

            auditLogger.log(

                    actor.getUsername(),

                    "SEND PLOT EMAIL",

                    "EMAIL",

                    "Plot was created successfully but "
                            + "email could not be sent.",

                    "FAILED"
            );
        }


        return ResponseEntity.ok(
                savedPlot
        );
    }


    /*
     * =========================================================
     * GET FARMER PLOTS
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','FARMER')")
    @GetMapping("/farmer/{farmerId}")
    public List<Plot> getFarmerPlots(

            @PathVariable Long farmerId

    ) {

        return plotRepository.findByFarmerId(
                farmerId
        );
    }


    /*
     * =========================================================
     * GET SUPERVISOR'S PLOTS
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


        return plotRepository
                .findByBlockOrderByIdDesc(
                        supervisor.getBlockName()
                );
    }


    /*
     * =========================================================
     * UPDATE PLOT
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlot(

            @PathVariable Long id,

            @RequestBody Plot updatedPlot,

            Authentication authentication

    ) {

        User actor =
                userRepository

                        .findByUsername(
                                authentication.getName()
                        )

                        .orElseThrow();


        return plotRepository.findById(id)

                .map(plot -> {


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
                                plot.getBlock() == null
                                        ||
                                        actor.getBlockName() == null
                                        ||
                                        !plot.getBlock()
                                                .equalsIgnoreCase(
                                                        actor.getBlockName()
                                                )
                        ) {

                            auditLogger.log(

                                    actor.getUsername(),

                                    "UPDATE PLOT",

                                    "PLOT",

                                    "Attempted to update plot outside assigned block",

                                    "FAILED"
                            );


                            return ResponseEntity
                                    .badRequest()
                                    .body(
                                            "You can only manage plots within your block"
                                    );
                        }
                    }


                    /*
                     * -----------------------------------------
                     * UPDATE FIELDS
                     * -----------------------------------------
                     */

                    plot.setSize(
                            updatedPlot.getSize()
                    );

                    plot.setSoilType(
                            updatedPlot.getSoilType()
                    );

                    plot.setLocationDescription(
                            updatedPlot
                                    .getLocationDescription()
                    );

                    plot.setIrrigationMethod(
                            updatedPlot
                                    .getIrrigationMethod()
                    );

                    plot.setSeason(
                            updatedPlot.getSeason()
                    );

                    plot.setStatus(
                            updatedPlot.getStatus()
                    );


                    plotRepository.save(plot);


                    /*
                     * -----------------------------------------
                     * AUDIT
                     * -----------------------------------------
                     */

                    auditLogger.log(

                            actor.getUsername(),

                            "UPDATE PLOT",

                            "PLOT",

                            "Updated Plot "
                                    + plot.getPlotNo(),

                            "SUCCESS"
                    );


                    /*
                     * -----------------------------------------
                     * FARMER NOTIFICATION
                     * -----------------------------------------
                     */

                    notificationService.notify(

                            plot.getFarmer(),

                            "Your plot "
                                    + plot.getPlotNo()
                                    + " information has been updated."
                    );


                    /*
                     * -----------------------------------------
                     * ACTOR NOTIFICATION
                     * -----------------------------------------
                     */

                    notificationService.notify(

                            actor,

                            "You updated Plot "
                                    + plot.getPlotNo()
                    );


                    /*
                     * -----------------------------------------
                     * GMAIL - FARMER
                     * -----------------------------------------
                     */

                    try {

                        emailService.sendPlotEmail(

                                plot.getFarmer(),

                                plot,

                                "UPDATED"
                        );


                        auditLogger.log(

                                actor.getUsername(),

                                "SEND PLOT EMAIL",

                                "EMAIL",

                                "Plot update email sent successfully to "
                                        + plot.getFarmer()
                                        .getEmail(),

                                "SUCCESS"
                        );

                    } catch (Exception e) {

                        auditLogger.log(

                                actor.getUsername(),

                                "SEND PLOT EMAIL",

                                "EMAIL",

                                "Plot was updated successfully but "
                                        + "email could not be sent.",

                                "FAILED"
                        );
                    }


                    return ResponseEntity.ok(
                            plot
                    );

                })

                .orElseGet(() -> {

                    auditLogger.log(

                            actor.getUsername(),

                            "UPDATE PLOT",

                            "PLOT",

                            "Plot not found",

                            "FAILED"
                    );


                    return ResponseEntity.notFound()
                            .build();
                });
    }


    /*
     * =========================================================
     * DELETE PLOT
     * =========================================================
     */

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlot(

            @PathVariable Long id,

            Authentication authentication

    ) {

        User actor =
                userRepository

                        .findByUsername(
                                authentication.getName()
                        )

                        .orElseThrow();


        Plot plot =
                plotRepository

                        .findById(id)

                        .orElse(null);


        if (plot == null) {

            auditLogger.log(

                    actor.getUsername(),

                    "DELETE PLOT",

                    "PLOT",

                    "Plot not found",

                    "FAILED"
            );


            return ResponseEntity.notFound()
                    .build();
        }


        /*
         * Save farmer reference before deleting plot.
         */

        User farmer =
                plot.getFarmer();

        String plotNo =
                plot.getPlotNo();


        /*
         * Delete plot.
         */

        plotRepository.delete(plot);


        /*
         * -----------------------------------------------------
         * AUDIT
         * -----------------------------------------------------
         */

        auditLogger.log(

                actor.getUsername(),

                "DELETE PLOT",

                "PLOT",

                "Deleted Plot "
                        + plotNo,

                "SUCCESS"
        );


        /*
         * -----------------------------------------------------
         * FARMER NOTIFICATION
         * -----------------------------------------------------
         */

        notificationService.notify(

                farmer,

                "Plot "
                        + plotNo
                        + " has been removed from your account."
        );


        /*
         * -----------------------------------------------------
         * ACTOR NOTIFICATION
         * -----------------------------------------------------
         */

        notificationService.notify(

                actor,

                "You deleted Plot "
                        + plotNo
        );


        /*
         * =====================================================
         * GMAIL - FARMER
         * =====================================================
         */

        try {

            emailService.sendPlotEmail(

                    farmer,

                    plot,

                    "DELETED"
            );


            auditLogger.log(

                    actor.getUsername(),

                    "SEND PLOT EMAIL",

                    "EMAIL",

                    "Plot deletion email sent successfully to "
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

                    "SEND PLOT EMAIL",

                    "EMAIL",

                    "Plot was deleted successfully but "
                            + "email could not be sent.",

                    "FAILED"
            );
        }


        return ResponseEntity.ok(

                "Plot deleted successfully"
        );
    }


    /*
     * =========================================================
     * FARMER'S OWN PLOTS
     * =========================================================
     */

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/my-farm-plots")
    public List<Plot> getMyFarmPlots(

            Authentication authentication

    ) {

        String username =
                authentication.getName();


        User farmer =
                userRepository

                        .findByUsername(username)

                        .orElseThrow();


        return plotRepository.findByFarmerId(
                farmer.getId()
        );
    }

}