package suza.irifams.user;

import org.springframework.data.jpa.repository.JpaRepository;
import suza.irifams.enums.Role;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    long countByRole(Role role);

    List<User> findByRole(String role);

}