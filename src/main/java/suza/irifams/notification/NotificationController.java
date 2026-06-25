package suza.irifams.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository repository;

    @GetMapping("/user/{userId}")
    public List<Notification> getUserNotifications(
            @PathVariable Long userId
    ) {

        return repository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    @GetMapping("/user/{userId}/unread-count")
    public long getUnreadCount(
            @PathVariable Long userId
    ) {

        return repository
                .countByUserIdAndIsReadFalse(userId);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id
    ) {

        return repository.findById(id)

                .map(notification -> {

                    notification.setRead(true);

                    repository.save(notification);

                    return ResponseEntity.ok(notification);

                })

                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

}