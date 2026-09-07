package suza.irifams.security.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification>
    findTopByUserUsernameOrderByCreatedAtDesc(
            String username
    );

    @Modifying
    @Query("""
        DELETE FROM OtpVerification o
        WHERE o.user.id = :userId
    """)
    void deleteByUserId(
            @Param("userId") Long userId
    );
}