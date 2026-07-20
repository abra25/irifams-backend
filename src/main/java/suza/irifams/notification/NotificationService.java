package suza.irifams.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import suza.irifams.user.User;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    public void notify(User user, String message){

        repository.save(

                Notification.builder()

                        .user(user)

                        .message(message)

                        .isRead(false)

                        .createdAt(LocalDateTime.now())

                        .build()

        );

    }

}
