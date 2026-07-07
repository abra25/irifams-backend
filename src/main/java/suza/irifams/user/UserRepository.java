package suza.irifams.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import suza.irifams.enums.Role;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    long countByRole(Role role);

    List<User> findByRole(String role);

    List<User> findTop5ByOrderByIdDesc();

    List<User> findByRole(
            suza.irifams.enums.Role role
    );
    List<User> findByRoleAndBlockName(
            Role role,
            String blockName
    );

    @Query("""
SELECT COUNT(u)
FROM User u
WHERE u.role='FARMER'
AND u.blockName=:block
""")
    Long countFarmersByBlock(String block);
}