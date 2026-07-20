package suza.irifams.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    List<Notification> findTop5ByOrderByCreatedAtDesc();

    List<Notification>
    findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteById(Long id);

}