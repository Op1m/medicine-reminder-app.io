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

import java.time.*;
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
        LocalTime currentTime = now.toLocalTime();

        List<Reminder> reminders = reminderRepository.findAllActiveWithUserAndMedicine();

        for (Reminder reminder : reminders) {

            if (reminder.getReminderTime() == null) continue;

            LocalTime reminderTime = reminder.getReminderTime();

            // ✅ фикс: окно ±60 секунд
            long diff = Math.abs(reminderTime.toSecondOfDay() - currentTime.toSecondOfDay());
            if (diff > 60) continue;

            // ✅ проверка даты
            if (reminder.getSpecificDate() != null) {
                if (!today.equals(reminder.getSpecificDate())) continue;
            } else {
                String days = reminder.getDaysOfWeek();
                if (days != null && !days.equals("everyday")) {
                    int todayNum = today.getDayOfWeek().getValue();
                    boolean match = Arrays.stream(days.split(","))
                            .map(String::trim)
                            .anyMatch(d -> d.equals(String.valueOf(todayNum)));
                    if (!match) continue;
                }
            }

            // ❗ проверяем только TAKEN / SKIPPED
            OffsetDateTime start = today.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = start.plusDays(1);

            boolean alreadyDone = medicineHistoryService
                    .getHistoryByPeriod(reminder.getUser().getId(), start, end)
                    .stream()
                    .anyMatch(h ->
                            h.getReminder().getId().equals(reminder.getId()) &&
                                    (h.getStatus() == MedicineStatus.TAKEN ||
                                            h.getStatus() == MedicineStatus.SKIPPED)
                    );

            if (alreadyDone) continue;

            // ✅ создаём history
            MedicineHistory history = medicineHistoryService.createScheduleDose(reminder.getId(), now);

            // ✅ отправляем
            if (history != null) {
                System.out.println("🚀 SEND reminder " + reminder.getId());
                notificationService.notifyUser(reminder);
            }
        }
    }

    @Scheduled(cron = "*/30 * * * * *")
    @Transactional
    public void checkPostponedReminders() {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<MedicineHistory> postponed = medicineHistoryService
                .getMedicineHistoryByStatus(null, MedicineStatus.POSTPONED);

        for (MedicineHistory history : postponed) {

            if (history.getScheduledTime() == null) continue;

            // ✅ фикс
            if (!history.getScheduledTime().isAfter(now)) {

                Reminder reminder = history.getReminder();
                if (reminder == null) continue;

                System.out.println("⏰ POSTPONED FIRE " + reminder.getId());

                notificationService.notifyUser(reminder);

                history.setStatus(MedicineStatus.PENDING);
                history.setScheduledTime(now);

                medicineHistoryService.save(history);
            }
        }
    }

    @Scheduled(cron = "10 * * * * *")
    @Transactional
    public void checkMissedDoses() {
        medicineHistoryService.checkAndMarkMissedDoses();
    }
}