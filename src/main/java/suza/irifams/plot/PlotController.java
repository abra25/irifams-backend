package suza.irifams.plot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
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

        if (plotRepository.existsByPlotNo(
                plot.getPlotNo())) {

            auditLogger.log(
                    "UNKNOWN",
                    "CREATE PLOT",
                    "PLOT",
                    "Failed to create plot. Plot number already exists",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body("Plot Number already exists");
        }

        User farmer =
                userRepository.findById(farmerId)
                        .orElse(null);

        if(farmer == null){

            auditLogger.log(
                    "UNKNOWN",
                    "CREATE PLOT",
                    "PLOT",
                    "Farmer not found",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body("Farmer not found");
        }

        String username =
                authentication.getName();

        User supervisor =
                userRepository.findByUsername(username)
                        .orElseThrow();

        // block automatic
        plot.setBlock(
                supervisor.getBlockName()
        );

        plot.setFarmer(farmer);

        Plot savedPlot =
                plotRepository.save(plot);

        auditLogger.log(
                farmer.getUsername(),
                "CREATE PLOT",
                "PLOT",
                "Plot "
                        + savedPlot.getPlotNo()
                        + " created successfully",
                "SUCCESS"
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

            @RequestBody Plot updatedPlot

    ) {

        return plotRepository.findById(id)

                .map(plot -> {

                    // block haisogezwi

                    plot.setSize(
                            updatedPlot.getSize());

                    plot.setSoilType(
                            updatedPlot.getSoilType());

                    plot.setLocationDescription(
                            updatedPlot.getLocationDescription());

                    plot.setIrrigationMethod(
                            updatedPlot.getIrrigationMethod());

                    plot.setSeason(
                            updatedPlot.getSeason());

                    plot.setStatus(
                            updatedPlot.getStatus());

                    auditLogger.log(
                            plot.getFarmer()
                                    .getUsername(),

                            "UPDATE PLOT",

                            "PLOT",

                            "Plot "
                                    + plot.getPlotNo()
                                    + " updated successfully",

                            "SUCCESS"
                    );

                    return ResponseEntity.ok(
                            plotRepository.save(plot)
                    );

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            "UNKNOWN",
                            "UPDATE PLOT",
                            "PLOT",
                            "Failed to update plot",
                            "FAILED"
                    );

                    return ResponseEntity.notFound()
                            .build();

                });

    }
    /*
     * DELETE PLOT
     */

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlot(
            @PathVariable Long id
    ) {

        if (!plotRepository.existsById(id)) {

            return ResponseEntity.notFound().build();

        }

        Plot plot =
                plotRepository.findById(id)
                        .orElse(null);

        if(plot == null){

            auditLogger.log(
                    "UNKNOWN",
                    "DELETE PLOT",
                    "PLOT",
                    "Failed to delete plot. Plot not found",
                    "FAILED"
            );

            return ResponseEntity.notFound().build();
        }

        auditLogger.log(
                plot.getFarmer().getUsername(),
                "DELETE PLOT",
                "PLOT",
                "Plot "
                        + plot.getPlotNo()
                        + " deleted successfully",
                "SUCCESS"
        );

        plotRepository.deleteById(id);

        return ResponseEntity.ok(
                "Plot deleted successfully");
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