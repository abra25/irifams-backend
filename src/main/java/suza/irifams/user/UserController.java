package suza.irifams.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import suza.irifams.audit.AuditLogger;
import suza.irifams.enums.Role;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    /*
     * GET ALL USERS
     */

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<User> getAllUsers() {

        return userRepository.findAll();

    }


    /*
     * GET USER BY ID
     */

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


    /*
     * CREATE USER
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestBody User user
    ) {

        if (userRepository.existsByUsername(
                user.getUsername())) {
            auditLogger.log(
                    user.getUsername(),
                    "CREATE USER",
                    "USER",
                    "Username already exists",
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body("Username already exists");

        }


        user.setPassword(
                passwordEncoder.encode(
                        user.getPassword()
                )
        );

        user.setEnabled(true);

        User savedUser =
                userRepository.save(user);

        auditLogger.log(
                savedUser.getUsername(),
                "CREATE USER",
                "USER",
                "User created successfully",
                "SUCCESS"
        );

        return ResponseEntity.ok(savedUser);
    }


    /*
     * UPDATE USER
     */

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','STAKEHOLDER')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(

            @PathVariable Long id,

            @RequestBody User updatedUser

    ) {

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
                    auditLogger.log(
                            user.getUsername(),
                            "UPDATE USER",
                            "USER",
                            "User updated successfully",
                            "SUCCESS"
                    );

                    return ResponseEntity.ok(
                            userRepository.save(user)
                    );



                })

                .orElseGet(() -> {

                    auditLogger.log(
                            "UNKNOWN",
                            "UPDATE USER",
                            "USER",
                            "Failed to update user. User not found",
                            "FAILED"
                    );

                    return ResponseEntity.notFound().build();

                });

    }


    /*
     * DELETE USER
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id
    ) {

        User user = userRepository
                .findById(id)
                .orElse(null);

        if (user == null) {

            auditLogger.log(
                    "UNKNOWN",
                    "DELETE USER",
                    "USER",
                    "Failed to delete user. User not found",
                    "FAILED"
            );

            return ResponseEntity.notFound().build();
        }

        auditLogger.log(
                user.getUsername(),
                "DELETE USER",
                "USER",
                "User deleted successfully",
                "SUCCESS"
        );

        userRepository.deleteById(id);

        return ResponseEntity.ok(
                "User deleted successfully"
        );
    }


    /*
     * ACTIVATE / DEACTIVATE USER
     */

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(
            @PathVariable Long id
    ) {

        return userRepository.findById(id)

                .map(user -> {

                    user.setEnabled(
                            !user.isEnabled()
                    );

                    userRepository.save(user);
                    auditLogger.log(
                            user.getUsername(),
                            "STATUS CHANGED",
                            "USER",
                            "User account status changed",
                            "SUCCESS"
                    );

                    return ResponseEntity.ok(user);

                })

                .orElseGet(() -> {

                    auditLogger.log(
                            "UNKNOWN",
                            "STATUS CHANGED",
                            "USER",
                            "Failed to change user status",
                            "FAILED"
                    );

                    return ResponseEntity.notFound().build();

                });

    }

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @GetMapping("/farmers")
    public List<User> getFarmers() {

        return userRepository.findByRole(
                suza.irifams.enums.Role.FARMER
        );

    }

    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR')")
    @GetMapping("/farmers/block/{blockName}")
    public List<User> getFarmersByBlock(
            @PathVariable String blockName
    ) {

        return userRepository.findByRoleAndBlockName(
                suza.irifams.enums.Role.FARMER,
                blockName
        );

    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/my-farmers")
    public List<User> getMyFarmers(
            Authentication authentication
    ) {

        // username wa supervisor aliyelogin
        String username =
                authentication.getName();

        // pata supervisor kwenye DB
        User supervisor = userRepository
                .findByUsername(username)
                .orElseThrow();

        // rudisha farmers wa block yake tu
        return userRepository
                .findByRoleAndBlockName(
                        Role.FARMER,
                        supervisor.getBlockName()
                );
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile(
            Authentication authentication
    ) {

        User user = userRepository

                .findByUsername(authentication.getName())

                .orElseThrow();

        return ResponseEntity.ok(user);

    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(

            Authentication authentication,

            @RequestBody ChangePasswordRequest request

    ){

        User user = userRepository

                .findByUsername(authentication.getName())

                .orElseThrow();

        if(!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword())){

            return ResponseEntity.badRequest()
                    .body("Current password is incorrect");

        }

        user.setPassword(

                passwordEncoder.encode(
                        request.getNewPassword()
                )

        );

        userRepository.save(user);

        auditLogger.log(

                user.getUsername(),

                "CHANGE PASSWORD",

                "USER",

                "Password changed successfully",

                "SUCCESS"

        );

        return ResponseEntity.ok(
                "Password updated successfully"
        );

    }

}
