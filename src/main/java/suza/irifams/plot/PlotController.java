package suza.irifams.plot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;
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

    /*
     * CREATE NEW PLOT
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PostMapping
    public ResponseEntity<?> addPlot(

            @RequestParam Long farmerId,

            @RequestBody Plot plot

    ) {

        if (plotRepository.existsByPlotNo(
                plot.getPlotNo())) {

            return ResponseEntity.badRequest()
                    .body("Plot Number already exists");

        }

        User farmer = userRepository.findById(farmerId)
                .orElseThrow();

        plot.setFarmer(farmer);

        // Save plot first
        Plot savedPlot = plotRepository.save(plot);

        // Audit log
        auditLogger.log(
                farmer.getUsername(),
                "CREATE",
                "PLOT",
                "Plot " +
                        savedPlot.getPlotNo() +
                        " created successfully"
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

                    plot.setBlock(
                            updatedPlot.getBlock());

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

                    return ResponseEntity.ok(
                            plotRepository.save(plot));

                })

                .orElse(
                        ResponseEntity.notFound().build());

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

        plotRepository.deleteById(id);

        return ResponseEntity.ok(
                "Plot deleted successfully");

    }

}