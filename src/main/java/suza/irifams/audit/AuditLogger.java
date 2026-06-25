package suza.irifams.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuditLogger {

    private final AuditLogRepository repository;

    public void log(

            String username,

            String action,

            String module,

            String description

    ) {

        repository.save(

                AuditLog.builder()

                        .username(username)

                        .action(action)

                        .module(module)

                        .description(description)

                        .createdAt(LocalDateTime.now())

                        .build()

        );

    }

}