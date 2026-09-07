package suza.irifams.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import suza.irifams.audit.AuditLogger;
import suza.irifams.email.EmailService;
import suza.irifams.enums.Role;
import suza.irifams.notification.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;
    private final EmailService emailService;


    // =========================================================
    // GET ALL USERS
    // =========================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<User> getAllUsers() {

        return userRepository.findAll();
    }


    // =========================================================
    // GET USER BY ID
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id
    ) {

        return userRepository.findById(id)

                .map(ResponseEntity::ok)

                .orElse(
                        ResponseEntity.notFound().build()
                );
    }


    // =========================================================
    // CREATE USER
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PostMapping
    public ResponseEntity<?> createUser(

            @RequestBody User user,

            Authentication authentication

    ) {

        User actor = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();


        // -----------------------------------------------------
        // Check username
        // -----------------------------------------------------

        if (
                userRepository.existsByUsername(
                        user.getUsername()
                )
        ) {

            auditLogger.log(
                    actor.getUsername(),
                    "CREATE USER",
                    "USER",
                    "Username already exists",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body("Username already exists");
        }


        // -----------------------------------------------------
        // Encode password
        // -----------------------------------------------------

        user.setPassword(
                passwordEncoder.encode(
                        user.getPassword()
                )
        );

        user.setEnabled(true);

        user.setTemporaryPassword(false);


        // -----------------------------------------------------
        // Save user
        // -----------------------------------------------------

        User savedUser =
                userRepository.save(user);


        // -----------------------------------------------------
        // Audit
        // -----------------------------------------------------

        auditLogger.log(
                actor.getUsername(),
                "CREATE USER",
                "USER",
                "Created user "
                        + savedUser.getUsername(),
                "SUCCESS"
        );


        // -----------------------------------------------------
        // Notification to new user
        // -----------------------------------------------------

        notificationService.notify(
                savedUser,
                "Your IRIFAMS account has been created successfully."
        );


        // -----------------------------------------------------
        // Notification to actor
        // -----------------------------------------------------

        notificationService.notify(
                actor,
                "You created account for "
                        + savedUser.getFullName()
        );


        // -----------------------------------------------------
        // Notify admins if actor is Supervisor
        // -----------------------------------------------------

        if (
                actor.getRole() == Role.SUPERVISOR
        ) {

            userRepository.findByRole(Role.ADMIN)
                    .forEach(admin ->

                            notificationService.notify(
                                    admin,
                                    actor.getFullName()
                                            + " created user "
                                            + savedUser.getFullName()
                            )
                    );
        }


        // -----------------------------------------------------
        // Send account creation email
        // -----------------------------------------------------

        try {

            emailService.sendAccountCreatedEmail(
                    savedUser
            );


            auditLogger.log(
                    actor.getUsername(),
                    "SEND ACCOUNT EMAIL",
                    "EMAIL",
                    "Account creation email sent successfully to "
                            + savedUser.getEmail(),
                    "SUCCESS"
            );


        } catch (Exception e) {

            auditLogger.log(
                    actor.getUsername(),
                    "SEND ACCOUNT EMAIL",
                    "EMAIL",
                    "User account was created successfully but "
                            + "account creation email could not be sent.",
                    "FAILED"
            );
        }


        return ResponseEntity.ok(
                savedUser
        );
    }


    // =========================================================
    // UPDATE USER
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(

            @PathVariable Long id,

            @RequestBody User updatedUser,

            Authentication authentication

    ) {

        User actor = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();


        return userRepository.findById(id)

                .map(user -> {

                    user.setFullName(
                            updatedUser.getFullName()
                    );

                    user.setPhone(
                            updatedUser.getPhone()
                    );

                    user.setEmail(
                            updatedUser.getEmail()
                    );

                    user.setGender(
                            updatedUser.getGender()
                    );

                    user.setBlockName(
                            updatedUser.getBlockName()
                    );

                    user.setInstitution(
                            updatedUser.getInstitution()
                    );

                    user.setRole(
                            updatedUser.getRole()
                    );

                    user.setImage(
                            updatedUser.getImage()
                    );


                    User saved =
                            userRepository.save(user);


                    // -------------------------------------------------
                    // Audit
                    // -------------------------------------------------

                    auditLogger.log(
                            actor.getUsername(),
                            "UPDATE USER",
                            "USER",
                            "Updated user "
                                    + saved.getUsername(),
                            "SUCCESS"
                    );


                    // -------------------------------------------------
                    // In-app notifications
                    // -------------------------------------------------

                    notificationService.notify(
                            saved,
                            "Your profile information has been updated."
                    );


                    notificationService.notify(
                            actor,
                            "You updated "
                                    + saved.getFullName()
                    );


                    // -------------------------------------------------
                    // Email notification
                    // -------------------------------------------------

                    try {

                        emailService.sendProfileUpdatedEmail(
                                saved
                        );


                        auditLogger.log(
                                actor.getUsername(),
                                "SEND PROFILE EMAIL",
                                "EMAIL",
                                "Profile update email sent successfully to "
                                        + saved.getEmail(),
                                "SUCCESS"
                        );


                    } catch (Exception e) {

                        auditLogger.log(
                                actor.getUsername(),
                                "SEND PROFILE EMAIL",
                                "EMAIL",
                                "Profile was updated successfully but "
                                        + "profile update email could not be sent.",
                                "FAILED"
                        );
                    }


                    return ResponseEntity.ok(
                            saved
                    );

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            actor.getUsername(),
                            "UPDATE USER",
                            "USER",
                            "User not found",
                            "FAILED"
                    );


                    return ResponseEntity.notFound()
                            .build();
                });
    }


    // =========================================================
    // DELETE USER
    // =========================================================

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(

            @PathVariable Long id,

            Authentication authentication

    ) {

        User actor = userRepository
                .findByUsername(
                        authentication.getName()
                )
                .orElseThrow();


        User user = userRepository
                .findById(id)
                .orElse(null);


        if (user == null) {

            auditLogger.log(
                    actor.getUsername(),
                    "DELETE USER",
                    "USER",
                    "User not found",
                    "FAILED"
            );

            return ResponseEntity.notFound()
                    .build();
        }


        auditLogger.log(
                actor.getUsername(),
                "DELETE USER",
                "USER",
                "Deleted user "
                        + user.getUsername(),
                "SUCCESS"
        );


        notificationService.notify(
                actor,
                "You deleted account for "
                        + user.getFullName()
        );


        userRepository.delete(user);


        return ResponseEntity.ok(
                "User deleted successfully"
        );
    }


    // =========================================================
    // ACTIVATE / DEACTIVATE USER
    // =========================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(

            @PathVariable Long id,

            Authentication authentication

    ) {

        User actor = userRepository
                .findByUsername(
                        authentication.getName()
                )
                .orElseThrow();


        return userRepository.findById(id)

                .map(user -> {

                    user.setEnabled(
                            !user.isEnabled()
                    );


                    userRepository.save(user);


                    // -------------------------------------------------
                    // Audit
                    // -------------------------------------------------

                    auditLogger.log(
                            actor.getUsername(),
                            "STATUS CHANGED",
                            "USER",
                            "Changed status for "
                                    + user.getUsername(),
                            "SUCCESS"
                    );


                    // -------------------------------------------------
                    // In-app notification
                    // -------------------------------------------------

                    notificationService.notify(
                            user,
                            user.isEnabled()
                                    ? "Your account has been activated."
                                    : "Your account has been deactivated."
                    );


                    notificationService.notify(
                            actor,
                            "You changed account status for "
                                    + user.getFullName()
                    );


                    // -------------------------------------------------
                    // Email notification
                    // -------------------------------------------------

                    try {

                        emailService.sendAccountStatusEmail(
                                user
                        );


                        auditLogger.log(
                                actor.getUsername(),
                                "SEND STATUS EMAIL",
                                "EMAIL",
                                "Account status email sent successfully to "
                                        + user.getEmail(),
                                "SUCCESS"
                        );


                    } catch (Exception e) {

                        auditLogger.log(
                                actor.getUsername(),
                                "SEND STATUS EMAIL",
                                "EMAIL",
                                "Account status was changed successfully but "
                                        + "status email could not be sent.",
                                "FAILED"
                        );
                    }


                    return ResponseEntity.ok(
                            user
                    );

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            actor.getUsername(),
                            "STATUS CHANGED",
                            "USER",
                            "User not found",
                            "FAILED"
                    );


                    return ResponseEntity.notFound()
                            .build();
                });
    }


    // =========================================================
    // GET ALL FARMERS
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @GetMapping("/farmers")
    public List<User> getFarmers() {

        return userRepository.findByRole(
                Role.FARMER
        );
    }


    // =========================================================
    // GET FARMERS BY BLOCK
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @GetMapping("/farmers/block/{blockName}")
    public List<User> getFarmersByBlock(

            @PathVariable String blockName

    ) {

        return userRepository.findByRoleAndBlockName(
                Role.FARMER,
                blockName
        );
    }


    // =========================================================
    // GET SUPERVISOR'S FARMERS
    // =========================================================

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/my-farmers")
    public List<User> getMyFarmers(

            Authentication authentication

    ) {

        String username =
                authentication.getName();


        User supervisor =
                userRepository
                        .findByUsername(username)
                        .orElseThrow();


        return userRepository
                .findByRoleAndBlockName(
                        Role.FARMER,
                        supervisor.getBlockName()
                );
    }


    // =========================================================
    // MY PROFILE
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile(

            Authentication authentication

    ) {

        User user =
                userRepository
                        .findByUsername(
                                authentication.getName()
                        )
                        .orElseThrow();


        return ResponseEntity.ok(user);
    }


    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(

            Authentication authentication,

            @RequestBody ChangePasswordRequest request

    ) {

        User user =
                userRepository
                        .findByUsername(
                                authentication.getName()
                        )
                        .orElseThrow();


        // -----------------------------------------------------
        // Verify current password
        // -----------------------------------------------------

        if (
                !passwordEncoder.matches(
                        request.getCurrentPassword(),
                        user.getPassword()
                )
        ) {

            auditLogger.log(
                    user.getUsername(),
                    "CHANGE PASSWORD",
                    "USER",
                    "Incorrect current password",
                    "FAILED"
            );


            return ResponseEntity.badRequest()
                    .body(
                            "Current password is incorrect"
                    );
        }


        // -----------------------------------------------------
        // Update password
        // -----------------------------------------------------

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );


        user.setTemporaryPassword(false);


        userRepository.save(user);


        // -----------------------------------------------------
        // Audit
        // -----------------------------------------------------

        auditLogger.log(
                user.getUsername(),
                "CHANGE PASSWORD",
                "USER",
                "Password changed successfully",
                "SUCCESS"
        );


        // -----------------------------------------------------
        // In-app notification
        // -----------------------------------------------------

        notificationService.notify(
                user,
                "Your account password has been changed successfully."
        );


        // -----------------------------------------------------
        // Gmail confirmation
        // -----------------------------------------------------

        try {

            emailService.sendPasswordChangedEmail(
                    user
            );


            auditLogger.log(
                    user.getUsername(),
                    "SEND PASSWORD CHANGE EMAIL",
                    "EMAIL",
                    "Password change confirmation email sent successfully to "
                            + user.getEmail(),
                    "SUCCESS"
            );


        } catch (Exception e) {

            auditLogger.log(
                    user.getUsername(),
                    "SEND PASSWORD CHANGE EMAIL",
                    "EMAIL",
                    "Password was changed successfully but "
                            + "confirmation email could not be sent.",
                    "FAILED"
            );
        }


        return ResponseEntity.ok(
                "Password updated successfully"
        );
    }
}