package suza.irifams.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository repository;
    private final UserRepository userRepository;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-notifications")
    public List<Notification> getMyNotifications(
            Authentication authentication
    ){

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();

        return repository.findByUserIdOrderByCreatedAtDesc(
                user.getId()
        );
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-unread-count")
    public long getUnreadCount(
            Authentication authentication
    ){

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();

        return repository.countByUserIdAndIsReadFalse(
                user.getId()
        );
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