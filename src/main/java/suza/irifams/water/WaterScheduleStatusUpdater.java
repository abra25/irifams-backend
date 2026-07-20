package suza.irifams.water;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import suza.irifams.enums.ScheduleStatus;
import suza.irifams.notification.NotificationService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WaterScheduleStatusUpdater {

    private final WaterScheduleRepository repository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void updateStatuses() {

        List<WaterSchedule> schedules = repository.findAll();

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        for (WaterSchedule schedule : schedules) {

            // ==========================================
            // 30 Minutes Reminder
            // ==========================================

            if (schedule.getStatus() == ScheduleStatus.UPCOMING
                    && !schedule.isReminderSent()
                    && schedule.getIrrigationDate().isEqual(today)) {

                LocalTime reminderTime =
                        schedule.getStartTime().minusMinutes(30);

                if (!now.isBefore(reminderTime)
                        && now.isBefore(schedule.getStartTime())) {

                    notificationService.notify(

                            schedule.getPlot().getFarmer(),

                            "Reminder! Your irrigation period for Plot "
                                    + schedule.getPlot().getPlotNo()
                                    + " will begin at "
                                    + schedule.getStartTime()

                    );

                    schedule.setReminderSent(true);

                    repository.save(schedule);
                }
            }

            // ==========================================
            // UPCOMING -> ACTIVE
            // ==========================================

            if (schedule.getStatus() == ScheduleStatus.UPCOMING
                    && schedule.getIrrigationDate().isEqual(today)
                    && !now.isBefore(schedule.getStartTime())) {

                schedule.setStatus(
                        ScheduleStatus.ACTIVE
                );

                if (!schedule.isStartedNotificationSent()) {

                    notificationService.notify(

                            schedule.getPlot().getFarmer(),

                            "Your irrigation period has started."

                    );

                    schedule.setStartedNotificationSent(true);
                }

                repository.save(schedule);
            }

            // ==========================================
            // ACTIVE -> COMPLETED
            // ==========================================

            if (schedule.getStatus() == ScheduleStatus.ACTIVE
                    && (

                    schedule.getIrrigationDate().isBefore(today)

                            ||

                            (

                                    schedule.getIrrigationDate().isEqual(today)

                                            &&

                                            now.isAfter(schedule.getEndTime())

                            )

            )) {

                schedule.setStatus(
                        ScheduleStatus.COMPLETED
                );

                if (!schedule.isCompletedNotificationSent()) {

                    notificationService.notify(

                            schedule.getPlot().getFarmer(),

                            "Your irrigation schedule has been completed."

                    );

                    schedule.setCompletedNotificationSent(true);
                }

                repository.save(schedule);
            }

        }

    }

}