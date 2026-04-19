package com.op1m.medrem.backend_api.scheduler;

import com.op1m.medrem.backend_api.entity.MedicineHistory;
import com.op1m.medrem.backend_api.entity.Reminder;
import com.op1m.medrem.backend_api.entity.enums.MedicineStatus;
import com.op1m.medrem.backend_api.service.MedicineHistoryService;
import com.op1m.medrem.backend_api.service.NotificationService;
import com.op1m.medrem.backend_api.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class ReminderScheduler {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private MedicineHistoryService medicineHistoryService;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
@Transactional
public void checkDueReminders() {
    System.out.println("Проверка напоминаний... " + OffsetDateTime.now(ZoneOffset.UTC));
    List<Reminder> dueReminders = reminderService.getDueReminders();
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

    for (Reminder reminder : dueReminders) {
        boolean alreadyExists = medicineHistoryService
                .getHistoryByPeriod(reminder.getUser().getId(), startOfDay, endOfDay)
                .stream()
                .anyMatch(h -> h.getReminder().getId().equals(reminder.getId())
                        && (h.getStatus() == MedicineStatus.PENDING
                        || h.getStatus() == MedicineStatus.POSTPONED
                        || h.getStatus() == MedicineStatus.TAKEN
                        || h.getStatus() == MedicineStatus.SKIPPED
                        || h.getStatus() == MedicineStatus.MISSED));

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

    private MedicineHistory createHistoryRecord(Reminder reminder) {
        try {
            MedicineHistory history = medicineHistoryService.createScheduleDose(
                reminder.getId(),
                OffsetDateTime.now(ZoneOffset.UTC)
            );
            System.out.println("Создана запись истории: " + history.getId() +
                ", время: " + OffsetDateTime.now(ZoneOffset.UTC));
            return history;
        } catch (Exception e) {
            System.out.println("Ошибка создания истории: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}