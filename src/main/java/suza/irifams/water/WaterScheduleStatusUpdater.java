package suza.irifams.water;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import suza.irifams.enums.ScheduleStatus;
import suza.irifams.notification.Notification;
import suza.irifams.notification.NotificationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WaterScheduleStatusUpdater {

    private final WaterScheduleRepository repository;
    private final NotificationRepository notificationRepository;

    @Scheduled(fixedRate = 60000) // every minute
    public void updateStatuses() {

        List<WaterSchedule> schedules = repository.findAll();

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        for (WaterSchedule schedule : schedules) {

            // ===============================
            // 30 Minutes Reminder
            // ===============================

            if (schedule.getStatus() == ScheduleStatus.UPCOMING
                    && !schedule.isReminderSent()
                    && schedule.getIrrigationDate().isEqual(today)) {

                LocalTime reminderTime =
                        schedule.getStartTime().minusMinutes(30);

                if (!now.isBefore(reminderTime)
                        && now.isBefore(schedule.getStartTime())) {

                    notificationRepository.save(

                            Notification.builder()

                                    .user(schedule.getPlot().getFarmer())

                                    .message(
                                            "Reminder! Your irrigation period for Plot "
                                                    + schedule.getPlot().getPlotNo()
                                                    + " will begin at "
                                                    + schedule.getStartTime()
                                    )

                                    .isRead(false)

                                    .createdAt(LocalDateTime.now())

                                    .build()

                    );

                    schedule.setReminderSent(true);

                    repository.save(schedule);
                }
            }

            // ===============================
            // UPCOMING -> ACTIVE
            // ===============================

            if (schedule.getStatus() == ScheduleStatus.UPCOMING
                    && schedule.getIrrigationDate().isEqual(today)
                    && !now.isBefore(schedule.getStartTime())) {

                schedule.setStatus(ScheduleStatus.ACTIVE);

                if (!schedule.isStartedNotificationSent()) {

                    notificationRepository.save(

                            Notification.builder()

                                    .user(schedule.getPlot().getFarmer())

                                    .message(
                                            "Your irrigation period has started."
                                    )

                                    .isRead(false)

                                    .createdAt(LocalDateTime.now())

                                    .build()

                    );

                    schedule.setStartedNotificationSent(true);
                }

                repository.save(schedule);
            }

            // ===============================
            // ACTIVE -> COMPLETED
            // ===============================

            if (schedule.getStatus() == ScheduleStatus.ACTIVE
                    && (

                    schedule.getIrrigationDate().isBefore(today)

                            ||

                            (schedule.getIrrigationDate().isEqual(today)
                                    && now.isAfter(schedule.getEndTime()))

            )) {

                schedule.setStatus(ScheduleStatus.COMPLETED);

                if (!schedule.isCompletedNotificationSent()) {

                    notificationRepository.save(

                            Notification.builder()

                                    .user(schedule.getPlot().getFarmer())

                                    .message(
                                            "Your irrigation schedule has been completed."
                                    )

                                    .isRead(false)

                                    .createdAt(LocalDateTime.now())

                                    .build()

                    );

                    schedule.setCompletedNotificationSent(true);
                }

                repository.save(schedule);
            }

        }

    }

}