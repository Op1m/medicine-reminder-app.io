package com.op1m.medrem.backend_api.scheduler;

import com.op1m.medrem.backend_api.entity.MedicineHistory;
import com.op1m.medrem.backend_api.entity.Reminder;
import com.op1m.medrem.backend_api.entity.enums.MedicineStatus;
import com.op1m.medrem.backend_api.repository.ReminderRepository;
import com.op1m.medrem.backend_api.service.MedicineHistoryService;
import com.op1m.medrem.backend_api.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Component
public class ReminderScheduler {

    @Autowired
    private MedicineHistoryService medicineHistoryService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void checkDueReminders() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime().withSecond(0).withNano(0);
        int currentDayOfWeek = today.getDayOfWeek().getValue();

        List<Reminder> allActive = reminderRepository.findAllActiveWithUserAndMedicine();

        for (Reminder reminder : allActive) {
            if (reminder.getReminderTime() == null) {
                continue;
            }

            LocalTime reminderTime = reminder.getReminderTime().withSecond(0).withNano(0);
            if (reminderTime.isAfter(currentTime) || reminderTime.isBefore(currentTime.minusMinutes(1))) {
                continue;
            }

            boolean shouldNotify;
            if (reminder.getSpecificDate() != null) {
                shouldNotify = today.equals(reminder.getSpecificDate());
            } else {
                String daysOfWeek = reminder.getDaysOfWeek();
                if (daysOfWeek == null || "everyday".equals(daysOfWeek)) {
                    shouldNotify = true;
                } else {
                    shouldNotify = Arrays.stream(daysOfWeek.split(","))
                            .map(String::trim)
                            .anyMatch(day -> String.valueOf(currentDayOfWeek).equals(day));
                }
            }

            if (!shouldNotify) {
                continue;
            }

            OffsetDateTime startOfDay = today.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

            boolean alreadyExists = medicineHistoryService
                    .getHistoryByPeriod(reminder.getUser().getId(), startOfDay, endOfDay)
                    .stream()
                    .anyMatch(h -> h.getReminder().getId().equals(reminder.getId())
                            && h.getScheduledTime() != null
                            && h.getScheduledTime().toLocalTime().withSecond(0).withNano(0).equals(currentTime)
                            && ( || h.getStatus() == MedicineStatus.TAKEN
                            || h.getStatus() == MedicineStatus.SKIPPED));

            if (alreadyExists) {
                continue;
            }

            MedicineHistory history = medicineHistoryService.createScheduleDose(reminder.getId(), now);

            if (history != null) {
                notificationService.notifyUser(reminder);
            }
        }
    }

    @Scheduled(cron = "5 * * * * *")
    @Transactional
    public void checkPostponedReminders() {
        medicineHistoryService.checkPostponedReminders();
    }

    @Scheduled(cron = "10 * * * * *")
    @Transactional
    public void checkMissedDoses() {
        medicineHistoryService.checkAndMarkMissedDoses();
    }
}