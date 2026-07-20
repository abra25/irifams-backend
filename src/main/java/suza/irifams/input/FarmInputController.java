package suza.irifams.input;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.enums.Role;
import suza.irifams.notification.NotificationService;
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
    private final NotificationService notificationService;

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

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PostMapping
    public FarmInput addInput(

            @RequestBody FarmInput input,

            Authentication authentication

    ){

        User actor = userRepository

                .findByUsername(authentication.getName())

                .orElseThrow();

        FarmInput saved =
                inputRepository.save(input);

        auditLogger.log(

                actor.getUsername(),

                "ADD INPUT",

                "INPUT",

                "Added farm input " + saved.getName(),

                "SUCCESS"

        );

        notificationService.notify(

                actor,

                "You added a new farm input: "
                        + saved.getName()

        );

        userRepository.findByRole(Role.ADMIN)

                .stream()

                .filter(admin -> !admin.getId().equals(actor.getId()))

                .forEach(admin ->

                        notificationService.notify(

                                admin,

                                "New farm input added: "
                                        + saved.getName()

                        )

                );

        return saved;
    }

    /*
     * UPDATE INPUT
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateInput(

            @PathVariable Long id,

            @RequestBody FarmInput updated,

            Authentication authentication

    ){
        return inputRepository.findById(id)

                .map(input -> {

                    User actor = userRepository

                            .findByUsername(authentication.getName())

                            .orElseThrow();

                    input.setName(updated.getName());
                    input.setCategory(updated.getCategory());
                    input.setQuantity(updated.getQuantity());
                    input.setUnit(updated.getUnit());
                    input.setSeason(updated.getSeason());
                    input.setImage(updated.getImage());
                    input.setStatus(updated.getStatus());

                    inputRepository.save(input);

                    auditLogger.log(

                            actor.getUsername(),

                            "UPDATE INPUT",

                            "INPUT",

                            "Updated input " + input.getName(),

                            "SUCCESS"

                    );

                    notificationService.notify(

                            actor,

                            "You updated farm input "
                                    + input.getName()

                    );

                    return ResponseEntity.ok(input);

                })

                .orElseGet(() -> {

                    auditLogger.log(

                            "UNKNOWN",

                            "UPDATE INPUT",

                            "INPUT",

                            "Input not found",

                            "FAILED"

                    );

                    return ResponseEntity.notFound().build();

                });

    }

    /*
     * DELETE INPUT
     */

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInput(

            @PathVariable Long id,

            Authentication authentication

    ){

        User actor = userRepository

                .findByUsername(authentication.getName())

                .orElseThrow();

        FarmInput input = inputRepository

                .findById(id)

                .orElse(null);

        if(input == null){

            auditLogger.log(

                    actor.getUsername(),

                    "DELETE INPUT",

                    "INPUT",

                    "Input not found",

                    "FAILED"

            );

            return ResponseEntity.notFound().build();

        }

        inputRepository.delete(input);

        auditLogger.log(

                actor.getUsername(),

                "DELETE INPUT",

                "INPUT",

                "Deleted input " + input.getName(),

                "SUCCESS"

        );

        notificationService.notify(

                actor,

                "You deleted farm input "
                        + input.getName()

        );

        return ResponseEntity.ok(
                "Input deleted successfully"
        );
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

                supervisor.getUsername(),

                "INPUT DISTRIBUTION",

                "INPUT",

                quantity + " " +
                        input.getUnit() +
                        " of " +
                        input.getName() +
                        " distributed to "
                        + farmer.getFullName(),

                "SUCCESS"

        );

// Farmer
        notificationService.notify(

                farmer,

                "You have received "
                        + quantity + " "
                        + input.getUnit()
                        + " of "
                        + input.getName()

        );

// Supervisor
        notificationService.notify(

                supervisor,

                "You distributed "
                        + input.getName()
                        + " to "
                        + farmer.getFullName()

        );

// Admins
        userRepository.findByRole(Role.ADMIN)

                .forEach(admin ->

                        notificationService.notify(

                                admin,

                                supervisor.getFullName()
                                        + " distributed "
                                        + input.getName()
                                        + " to "
                                        + farmer.getFullName()

                        )

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