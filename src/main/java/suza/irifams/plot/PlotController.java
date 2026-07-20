package suza.irifams.plot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
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

    /*
     * GET ALL PLOTS
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','STAKEHOLDER')")
    @GetMapping
    public List<Plot> getAllPlots() {

        return plotRepository.findAll();

    }

    /*
     * GET PLOT BY ID
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','STAKEHOLDER','FARMER')")
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
     * CREATE NEW PLOT
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PostMapping
    public ResponseEntity<?> addPlot(

            @RequestParam Long farmerId,

            @RequestBody Plot plot,

            Authentication authentication

    ) {

        User actor = userRepository

                .findByUsername(authentication.getName())

                .orElseThrow();

        if (plotRepository.existsByPlotNo(plot.getPlotNo())) {

            auditLogger.log(

                    actor.getUsername(),

                    "CREATE PLOT",

                    "PLOT",

                    "Plot number already exists",

                    "FAILED"

            );

            return ResponseEntity.badRequest()

                    .body("Plot Number already exists");
        }

        User farmer = userRepository

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

                    .body("Farmer not found");
        }

        plot.setBlock(actor.getBlockName());

        plot.setFarmer(farmer);

        Plot savedPlot = plotRepository.save(plot);

        auditLogger.log(

                actor.getUsername(),

                "CREATE PLOT",

                "PLOT",

                "Created plot " + savedPlot.getPlotNo(),

                "SUCCESS"

        );

        notificationService.notify(

                farmer,

                "A new plot (" + savedPlot.getPlotNo() + ") has been assigned to your account."

        );

        notificationService.notify(

                actor,

                "You successfully created Plot " + savedPlot.getPlotNo()

        );

        userRepository.findByRole(Role.ADMIN)

                .stream()

                .filter(admin -> !admin.getId().equals(actor.getId()))

                .forEach(admin ->

                        notificationService.notify(

                                admin,

                                actor.getFullName()

                                        + " created Plot "

                                        + savedPlot.getPlotNo()

                        )

                );

        return ResponseEntity.ok(savedPlot);

    }

    /*
     * GET FARMER PLOTS
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
     * UPDATE PLOT
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlot(

            @PathVariable Long id,

            @RequestBody Plot updatedPlot,

            Authentication authentication

    ){

        User actor = userRepository

                .findByUsername(authentication.getName())

                .orElseThrow();

        return plotRepository.findById(id)

                .map(plot -> {

                    plot.setSize(updatedPlot.getSize());

                    plot.setSoilType(updatedPlot.getSoilType());

                    plot.setLocationDescription(updatedPlot.getLocationDescription());

                    plot.setIrrigationMethod(updatedPlot.getIrrigationMethod());

                    plot.setSeason(updatedPlot.getSeason());

                    plot.setStatus(updatedPlot.getStatus());

                    plotRepository.save(plot);

                    auditLogger.log(

                            actor.getUsername(),

                            "UPDATE PLOT",

                            "PLOT",

                            "Updated Plot " + plot.getPlotNo(),

                            "SUCCESS"

                    );

                    notificationService.notify(

                            plot.getFarmer(),

                            "Your plot "

                                    + plot.getPlotNo()

                                    + " information has been updated."

                    );

                    notificationService.notify(

                            actor,

                            "You updated Plot "

                                    + plot.getPlotNo()

                    );

                    return ResponseEntity.ok(plot);

                })

                .orElseGet(() -> {

                    auditLogger.log(

                            actor.getUsername(),

                            "UPDATE PLOT",

                            "PLOT",

                            "Plot not found",

                            "FAILED"

                    );

                    return ResponseEntity.notFound().build();

                });

    }
    /*
     * DELETE PLOT
     */

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlot(

            @PathVariable Long id,

            Authentication authentication

    ){

        User actor = userRepository

                .findByUsername(authentication.getName())

                .orElseThrow();

        Plot plot = plotRepository

                .findById(id)

                .orElse(null);

        if(plot == null){

            auditLogger.log(

                    actor.getUsername(),

                    "DELETE PLOT",

                    "PLOT",

                    "Plot not found",

                    "FAILED"

            );

            return ResponseEntity.notFound().build();

        }

        plotRepository.delete(plot);

        auditLogger.log(

                actor.getUsername(),

                "DELETE PLOT",

                "PLOT",

                "Deleted Plot " + plot.getPlotNo(),

                "SUCCESS"

        );

        notificationService.notify(

                plot.getFarmer(),

                "Plot "

                        + plot.getPlotNo()

                        + " has been removed from your account."

        );

        notificationService.notify(

                actor,

                "You deleted Plot "

                        + plot.getPlotNo()

        );

        return ResponseEntity.ok(

                "Plot deleted successfully"

        );

    }


    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/my-farm-plots")
    public List<Plot> getMyFarmPlots(
            Authentication authentication
    ){

        String username =
                authentication.getName();

        User farmer = userRepository
                .findByUsername(username)
                .orElseThrow();

        return plotRepository.findByFarmerId(
                farmer.getId()
        );
    }
}