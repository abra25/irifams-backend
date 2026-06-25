package suza.irifams.input;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/inputs")
@RequiredArgsConstructor
public class FarmInputController {

    private final FarmInputRepository inputRepository;
    private final InputDistributionRepository distributionRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    /*
     * ALL INPUTS
     */

    @GetMapping
    public List<FarmInput> getAllInputs() {

        return inputRepository.findAll();

    }

    /*
     * ADD INPUT
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @PostMapping
    public FarmInput addInput(
            @RequestBody FarmInput input
    ) {

        return inputRepository.save(input);

    }

    /*
     * UPDATE INPUT
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateInput(

            @PathVariable Long id,

            @RequestBody FarmInput updated

    ) {

        return inputRepository.findById(id)

                .map(input -> {

                    input.setName(updated.getName());
                    input.setCategory(updated.getCategory());
                    input.setQuantity(updated.getQuantity());
                    input.setUnit(updated.getUnit());
                    input.setSeason(updated.getSeason());
                    input.setImage(updated.getImage());
                    input.setStatus(updated.getStatus());

                    return ResponseEntity.ok(
                            inputRepository.save(input)
                    );

                })

                .orElse(
                        ResponseEntity.notFound().build()
                );

    }

    /*
     * DELETE INPUT
     */

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteInput(
            @PathVariable Long id
    ) {

        inputRepository.deleteById(id);

    }

    /*
     * DISTRIBUTE INPUT
     */

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @PostMapping("/{inputId}/distribute")
    public ResponseEntity<?> distributeInput(

            @PathVariable Long inputId,

            @RequestParam Long farmerId,

            @RequestParam Double quantity

    ) {

        FarmInput input = inputRepository
                .findById(inputId)
                .orElseThrow();

        User farmer = userRepository
                .findById(farmerId)
                .orElseThrow();

        if (input.getQuantity() < quantity) {

            return ResponseEntity.badRequest()
                    .body("Insufficient stock");

        }

        input.setQuantity(
                input.getQuantity() - quantity
        );

        if (input.getQuantity() <= 0) {

            input.setStatus("Out of Stock");

        }

        inputRepository.save(input);

        InputDistribution distribution =
                InputDistribution.builder()

                        .quantity(quantity)

                        .season(input.getSeason())

                        .distributedAt(
                                LocalDateTime.now()
                        )

                        .farmInput(input)

                        .farmer(farmer)

                        .build();

        distributionRepository.save(distribution);

        auditLogger.log(
                farmer.getUsername(),
                "INPUT_DISTRIBUTION",
                "INPUT",
                "Received "
                        + quantity
                        + " "
                        + input.getUnit()
                        + " of "
                        + input.getName()
        );

        return ResponseEntity.ok(
                "Input distributed successfully"
        );

    }

    /*
     * FARMER DISTRIBUTIONS
     */

    @PreAuthorize(
            "hasAnyRole('FARMER','ADMIN','SUPERVISOR')")
    @GetMapping("/farmer/{farmerId}")
    public List<InputDistribution>
    getFarmerInputs(
            @PathVariable Long farmerId
    ) {

        return distributionRepository
                .findByFarmerId(farmerId);

    }

}