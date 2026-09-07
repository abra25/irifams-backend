package suza.irifams.input;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import suza.irifams.audit.AuditLogger;
import suza.irifams.email.EmailService;
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
    private final EmailService emailService;


    /*
     * =========================================================
     * ALL INPUTS
     * =========================================================
     */

    @GetMapping
    public List<FarmInput> getAllInputs() {

        return inputRepository.findAll();
    }


    /*
     * =========================================================
     * ADD INPUT
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PostMapping
    public FarmInput addInput(

            @RequestBody FarmInput input,

            Authentication authentication

    ) {

        User actor = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();


        FarmInput saved =
                inputRepository.save(input);


        // -----------------------------------------------------
        // AUDIT
        // -----------------------------------------------------

        auditLogger.log(

                actor.getUsername(),

                "ADD INPUT",

                "INPUT",

                "Added farm input "
                        + saved.getName(),

                "SUCCESS"
        );


        // -----------------------------------------------------
        // NOTIFICATION TO ACTOR
        // -----------------------------------------------------

        notificationService.notify(

                actor,

                "You added a new farm input: "
                        + saved.getName()
        );


        // -----------------------------------------------------
        // NOTIFY OTHER ADMINS
        // -----------------------------------------------------

        userRepository.findByRole(Role.ADMIN)

                .stream()

                .filter(
                        admin ->
                                !admin.getId()
                                        .equals(actor.getId())
                )

                .forEach(
                        admin ->

                                notificationService.notify(

                                        admin,

                                        "New farm input added: "
                                                + saved.getName()
                                )
                );


        return saved;
    }


    /*
     * =========================================================
     * UPDATE INPUT
     * =========================================================
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateInput(

            @PathVariable Long id,

            @RequestBody FarmInput updated,

            Authentication authentication

    ) {

        return inputRepository.findById(id)

                .map(input -> {

                    User actor = userRepository

                            .findByUsername(
                                    authentication.getName()
                            )

                            .orElseThrow();


                    input.setName(
                            updated.getName()
                    );

                    input.setCategory(
                            updated.getCategory()
                    );

                    input.setQuantity(
                            updated.getQuantity()
                    );

                    input.setUnit(
                            updated.getUnit()
                    );

                    input.setSeason(
                            updated.getSeason()
                    );

                    input.setImage(
                            updated.getImage()
                    );

                    input.setStatus(
                            updated.getStatus()
                    );


                    inputRepository.save(input);


                    // -------------------------------------------------
                    // AUDIT
                    // -------------------------------------------------

                    auditLogger.log(

                            actor.getUsername(),

                            "UPDATE INPUT",

                            "INPUT",

                            "Updated input "
                                    + input.getName(),

                            "SUCCESS"
                    );


                    // -------------------------------------------------
                    // NOTIFICATION
                    // -------------------------------------------------

                    notificationService.notify(

                            actor,

                            "You updated farm input "
                                    + input.getName()
                    );


                    return ResponseEntity.ok(
                            input
                    );

                })

                .orElseGet(() -> {

                    auditLogger.log(

                            authentication.getName(),

                            "UPDATE INPUT",

                            "INPUT",

                            "Input not found",

                            "FAILED"
                    );


                    return ResponseEntity.notFound()
                            .build();
                });
    }


    /*
     * =========================================================
     * DELETE INPUT
     * =========================================================
     */

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInput(

            @PathVariable Long id,

            Authentication authentication

    ) {

        User actor = userRepository

                .findByUsername(
                        authentication.getName()
                )

                .orElseThrow();


        FarmInput input =
                inputRepository

                        .findById(id)

                        .orElse(null);


        if (input == null) {

            auditLogger.log(

                    actor.getUsername(),

                    "DELETE INPUT",

                    "INPUT",

                    "Input not found",

                    "FAILED"
            );

            return ResponseEntity.notFound()
                    .build();
        }


        String inputName =
                input.getName();


        inputRepository.delete(input);


        // -----------------------------------------------------
        // AUDIT
        // -----------------------------------------------------

        auditLogger.log(

                actor.getUsername(),

                "DELETE INPUT",

                "INPUT",

                "Deleted input "
                        + inputName,

                "SUCCESS"
        );


        // -----------------------------------------------------
        // NOTIFICATION
        // -----------------------------------------------------

        notificationService.notify(

                actor,

                "You deleted farm input "
                        + inputName
        );


        return ResponseEntity.ok(
                "Input deleted successfully"
        );
    }


    /*
     * =========================================================
     * DISTRIBUTE INPUT
     * =========================================================
     */

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping("/{inputId}/distribute")
    public ResponseEntity<?> distributeInput(

            @PathVariable Long inputId,

            @RequestParam Long farmerId,

            @RequestParam Double quantity,

            Authentication authentication

    ) {

        String username =
                authentication.getName();


        User supervisor =
                userRepository

                        .findByUsername(username)

                        .orElseThrow();


        FarmInput input =
                inputRepository

                        .findById(inputId)

                        .orElseThrow();


        User farmer =
                userRepository

                        .findById(farmerId)

                        .orElseThrow();


        // -----------------------------------------------------
        // CHECK FARMER BLOCK
        // -----------------------------------------------------

        if (
                farmer.getBlockName() == null
                        ||
                        supervisor.getBlockName() == null
                        ||
                        !farmer.getBlockName()
                                .equals(
                                        supervisor.getBlockName()
                                )
        ) {

            auditLogger.log(

                    supervisor.getUsername(),

                    "INPUT DISTRIBUTE",

                    "INPUT",

                    "Attempted to distribute input to farmer "
                            + "outside assigned block",

                    "FAILED"
            );


            return ResponseEntity.badRequest()

                    .body(
                            "You can distribute only within your block"
                    );
        }


        // -----------------------------------------------------
        // CHECK QUANTITY
        // -----------------------------------------------------

        if (
                quantity == null
                        ||
                        quantity <= 0
        ) {

            auditLogger.log(

                    supervisor.getUsername(),

                    "INPUT DISTRIBUTION",

                    "INPUT",

                    "Invalid input quantity",

                    "FAILED"
            );


            return ResponseEntity.badRequest()

                    .body(
                            "Quantity must be greater than zero"
                    );
        }


        if (
                input.getQuantity() == null
                        ||
                        input.getQuantity() < quantity
        ) {

            auditLogger.log(

                    supervisor.getUsername(),

                    "INPUT DISTRIBUTION",

                    "INPUT",

                    "Insufficient stock",

                    "FAILED"
            );


            return ResponseEntity.badRequest()

                    .body(
                            "Insufficient stock"
                    );
        }


        // -----------------------------------------------------
        // REDUCE STOCK
        // -----------------------------------------------------

        input.setQuantity(

                input.getQuantity()
                        - quantity
        );


        if (
                input.getQuantity() <= 0
        ) {

            input.setStatus(
                    "Out of Stock"
            );
        }


        inputRepository.save(input);


        // -----------------------------------------------------
        // CREATE DISTRIBUTION
        // -----------------------------------------------------

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


        distributionRepository.save(
                distribution
        );


        // -----------------------------------------------------
        // AUDIT
        // -----------------------------------------------------

        auditLogger.log(

                supervisor.getUsername(),

                "INPUT DISTRIBUTION",

                "INPUT",

                quantity
                        + " "
                        + input.getUnit()
                        + " of "
                        + input.getName()
                        + " distributed to "
                        + farmer.getFullName(),

                "SUCCESS"
        );


        // -----------------------------------------------------
        // NOTIFICATION TO FARMER
        // -----------------------------------------------------

        notificationService.notify(

                farmer,

                "You have received "
                        + quantity
                        + " "
                        + input.getUnit()
                        + " of "
                        + input.getName()
        );


        // -----------------------------------------------------
        // NOTIFICATION TO SUPERVISOR
        // -----------------------------------------------------

        notificationService.notify(

                supervisor,

                "You distributed "
                        + input.getName()
                        + " to "
                        + farmer.getFullName()
        );


        // -----------------------------------------------------
        // NOTIFICATION TO ADMINS
        // -----------------------------------------------------

        userRepository.findByRole(Role.ADMIN)

                .forEach(

                        admin ->

                                notificationService.notify(

                                        admin,

                                        supervisor.getFullName()
                                                + " distributed "
                                                + input.getName()
                                                + " to "
                                                + farmer.getFullName()
                                )
                );


        // =====================================================
        // GMAIL - FARMER INPUT DISTRIBUTION
        // =====================================================

        try {

            emailService.sendInputDistributionEmail(

                    farmer,

                    input,

                    quantity
            );


            auditLogger.log(

                    supervisor.getUsername(),

                    "SEND INPUT EMAIL",

                    "EMAIL",

                    "Input distribution email sent successfully to "
                            + farmer.getEmail(),

                    "SUCCESS"
            );

        } catch (Exception e) {

            /*
             * Email failure must NOT cancel
             * the successful input distribution.
             */

            auditLogger.log(

                    supervisor.getUsername(),

                    "SEND INPUT EMAIL",

                    "EMAIL",

                    "Input was distributed successfully but "
                            + "distribution email could not be sent.",

                    "FAILED"
            );
        }


        return ResponseEntity.ok(

                java.util.Map.of(

                        "message",
                        "Input distributed successfully"

                )
        );
    }


    /*
     * =========================================================
     * FARMER DISTRIBUTIONS
     * =========================================================
     */

    @PreAuthorize(
            "hasAnyRole('FARMER','ADMIN','SUPERVISOR')"
    )
    @GetMapping("/farmer/{farmerId}")
    public List<InputDistribution> getFarmerInputs(

            @PathVariable Long farmerId

    ) {

        return distributionRepository
                .findByFarmerId(farmerId);
    }

}