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

    /** Проверка всех обычных PENDING reminders по расписанию */
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

            // Проверяем совпадение времени с учётом 60 секунд
            long diff = Math.abs(reminderTime.toSecondOfDay() - currentTime.toSecondOfDay());
            if (diff > 60) continue;

            // Проверяем, подходит ли сегодняшняя дата
            if (reminder.getSpecificDate() != null) {
                // Курсовое напоминание
                if (!today.equals(reminder.getSpecificDate())) continue;
            } else {
                // Обычное напоминание с днями недели
                String days = reminder.getDaysOfWeek();
                if (days != null && !days.equals("everyday")) {
                    int todayNum = today.getDayOfWeek().getValue();
                    boolean match = Arrays.stream(days.split(","))
                            .map(String::trim)
                            .anyMatch(d -> d.equals(String.valueOf(todayNum)));
                    if (!match) continue;
                }
            }

            // Проверяем, была ли уже запись за сегодня со статусом TAKEN или SKIPPED
            OffsetDateTime startOfDay = today.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

            boolean alreadyTakenOrSkipped = medicineHistoryService
                    .getHistoryByPeriod(reminder.getUser().getId(), startOfDay, endOfDay)
                    .stream()
                    .anyMatch(h -> h.getReminder().getId().equals(reminder.getId())
                            && (h.getStatus() == MedicineStatus.TAKEN || h.getStatus() == MedicineStatus.SKIPPED));

            if (alreadyTakenOrSkipped) {
                System.out.println("[SKIP] Reminder " + reminder.getId() + " already taken or skipped today");
                continue;
            }

            // Проверяем, есть ли уже POSTPONED запись на сегодня (если есть — не создаём новую)
            boolean alreadyPostponed = medicineHistoryService
                    .getHistoryByPeriod(reminder.getUser().getId(), startOfDay, endOfDay)
                    .stream()
                    .anyMatch(h -> h.getReminder().getId().equals(reminder.getId())
                            && h.getStatus() == MedicineStatus.POSTPONED);

            if (alreadyPostponed) {
                System.out.println("[SKIP] Reminder " + reminder.getId() + " already postponed today");
                continue;
            }

            // Создаём новую запись PENDING
            MedicineHistory history = medicineHistoryService.createScheduleDose(reminder.getId(), now);
            System.out.println("[PENDING] Created history " + history.getId() + " for reminder " + reminder.getId());

            // Отправляем уведомление
            notificationService.notifyUser(reminder);
        }
    }

    /** Проверка отложенных reminders (POSTPONED) */
    @Scheduled(cron = "*/30 * * * * *") // каждые 30 секунд
    @Transactional
    public void checkPostponedReminders() {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<MedicineHistory> postponed = medicineHistoryService
                .getMedicineHistoryByStatus(null, MedicineStatus.POSTPONED);

        for (MedicineHistory history : postponed) {

            Reminder reminder = history.getReminder();
            if (reminder == null) continue;

            // логируем POSTPONED
            System.out.println("[POSTPONED] Reminder ID: " + reminder.getId() +
                    ", User ID: " + reminder.getUser().getId() +
                    ", chatId: " + reminder.getUser().getTelegramChatId() +
                    ", now: " + now +
                    ", scheduledTime: " + history.getScheduledTime() +
                    ", specificDate: " + reminder.getSpecificDate() +
                    ", historyId: " + history.getId());

            if (history.getScheduledTime() == null) continue;

            if (!history.getScheduledTime().isAfter(now)) {

                System.out.println("[POSTPONED FIRE] Sending reminder ID: " + reminder.getId());

                // отправляем
                notificationService.notifyUser(reminder);

                // переводим обратно в PENDING
                history.setStatus(MedicineStatus.PENDING);
                medicineHistoryService.save(history);
            }
        }
    }

    /** Проверка пропущенных доз */
    @Scheduled(cron = "10 * * * * *") // каждая минута +10 секунд
    @Transactional
    public void checkMissedDoses() {
        medicineHistoryService.checkAndMarkMissedDoses();
    }
}