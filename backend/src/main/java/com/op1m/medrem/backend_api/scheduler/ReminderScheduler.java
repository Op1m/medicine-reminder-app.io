package com.op1m.medrem.backend_api.scheduler;

import com.op1m.medrem.backend_api.entity.MedicineHistory;
import com.op1m.medrem.backend_api.entity.Reminder;
import com.op1m.medrem.backend_api.entity.enums.MedicineStatus;
import com.op1m.medrem.backend_api.repository.ReminderRepository;
import com.op1m.medrem.backend_api.service.MedicineHistoryService;
import com.op1m.medrem.backend_api.service.NotificationService;
import com.op1m.medrem.backend_api.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Arrays;
import java.util.List;

@Component
public class ReminderScheduler {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private MedicineHistoryService medicineHistoryService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
private ReminderRepository reminderRepository;

@Scheduled(cron = "0 * * * * *")
@Transactional
public void checkDueReminders() {
    LocalDateTime now = LocalDateTime.now();
    LocalDate today = now.toLocalDate();
    LocalTime currentTime = now.toLocalTime().withSecond(0).withNano(0);
    int currentDayOfWeek = today.getDayOfWeek().getValue();

    OffsetDateTime startOfDay = today.atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

    List<Reminder> allActive = reminderRepository.findAllActiveWithUserAndMedicine();

    for (Reminder reminder : allActive) {
        // 1. Проверка времени
        if (reminder.getReminderTime() == null ||
            !reminder.getReminderTime().equals(currentTime)) {
            continue;
        }

        // 2. Проверка даты/дня недели
        boolean shouldNotify = false;

        if (reminder.getSpecificDate() != null) {
            // Курсовое напоминание
            shouldNotify = today.equals(reminder.getSpecificDate());
        } else {
            // Обычное напоминание
            String daysOfWeek = reminder.getDaysOfWeek();
            if ("everyday".equals(daysOfWeek)) {
                shouldNotify = true;
            } else if (daysOfWeek != null) {
                shouldNotify = Arrays.stream(daysOfWeek.split(","))
                        .map(String::trim)
                        .anyMatch(day -> String.valueOf(currentDayOfWeek).equals(day));
            }
        }

        if (!shouldNotify) {
            continue;
        }

        // 3. Проверка, не создана ли уже запись в истории
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

        // 4. Создаём запись и отправляем уведомление
        MedicineHistory history = medicineHistoryService.createScheduleDose(
                reminder.getId(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

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