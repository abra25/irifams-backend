package suza.irifams.input;
import org.springframework.security.core.Authentication;
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

        FarmInput saved =
                inputRepository.save(input);

        auditLogger.log(
                "ADMIN",
                "ADD INPUT",
                "INPUT",
                "Added input "
                        + saved.getName(),
                "SUCCESS"
        );

        return saved;
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
                    auditLogger.log(
                            "ADMIN",
                            "UPDATE INPUT",
                            "INPUT",
                            "Updated input "
                                    + input.getName(),
                            "SUCCESS"
                    );

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
        FarmInput input =
                inputRepository.findById(id)
                        .orElse(null);

        if(input == null){

            auditLogger.log(
                    "UNKNOWN",
                    "DELETE INPUT",
                    "INPUT",
                    "Failed to delete input",
                    "FAILED"
            );

            return;
        }

        auditLogger.log(
                "ADMIN",
                "DELETE INPUT",
                "INPUT",
                "Deleted input "
                        + input.getName(),
                "SUCCESS"
        );

        inputRepository.deleteById(id);

    }

    /*
     * DISTRIBUTE INPUT
     */

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping("/{inputId}/distribute")
    public ResponseEntity<?> distributeInput(

            @PathVariable Long inputId,

            @RequestParam Long farmerId,

            @RequestParam Double quantity,

            Authentication authentication

    ) {

        String username = authentication.getName();

        User supervisor = userRepository
                .findByUsername(username)
                .orElseThrow();

        FarmInput input = inputRepository
                .findById(inputId)
                .orElseThrow();

        User farmer = userRepository
                .findById(farmerId)
                .orElseThrow();

        // hakikisha farmer ni wa block ya supervisor

        if(!farmer.getBlockName()
                .equals(supervisor.getBlockName())){

            auditLogger.log(
                    supervisor.getUsername(),
                    "INPUT DISTRIBUTE",
                    "INPUT",
                    "Attempted to distribute input to farmer outside assigned block",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body("You can distribute only within your block");
        }

        if(input.getQuantity() < quantity){

            auditLogger.log(
                    farmer.getUsername(),
                    "INPUT DISTRIBUTION",
                    "INPUT",
                    "Insufficient stock",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body("Insufficient stock");
        }

        input.setQuantity(
                input.getQuantity() - quantity
        );

        if(input.getQuantity() <= 0){

            input.setStatus("Out of Stock");
        }

        inputRepository.save(input);

        InputDistribution distribution =
                InputDistribution.builder()

                        .farmInput(input)

                        .farmer(farmer)

                        .quantity(quantity)

                        .season(input.getSeason())

                        .distributedAt(
                                LocalDateTime.now()
                        )

                        .build();

        distributionRepository.save(distribution);

        auditLogger.log(
                farmer.getUsername(),
                "INPUT DISTRIBUTION",
                "INPUT",
                quantity + " " +
                        input.getUnit() +
                        " of " +
                        input.getName() +
                        " distributed",
                "SUCCESS"
        );

        return ResponseEntity.ok(
                java.util.Map.of(
                        "message",
                        "Input distributed successfully"
                )
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