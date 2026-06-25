package suza.irifams.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

            return ResponseEntity.badRequest()
                    .body("Username already exists");

        }

        user.setPassword(
                passwordEncoder.encode(
                        user.getPassword()
                )
        );

        user.setEnabled(true);

        return ResponseEntity.ok(
                userRepository.save(user)
        );

    }


    /*
     * UPDATE USER
     */

    @PreAuthorize("hasRole('ADMIN')")
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

                    return ResponseEntity.ok(
                            userRepository.save(user)
                    );

                })

                .orElse(
                        ResponseEntity.notFound().build()
                );

    }


    /*
     * DELETE USER
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id
    ) {

        if (!userRepository.existsById(id)) {

            return ResponseEntity.notFound().build();

        }

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

                    return ResponseEntity.ok(user);

                })

                .orElse(
                        ResponseEntity.notFound().build()
                );

    }

}
