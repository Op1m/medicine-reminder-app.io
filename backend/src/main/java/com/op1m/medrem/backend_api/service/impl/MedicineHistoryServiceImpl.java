package com.op1m.medrem.backend_api.service.impl;

import com.op1m.medrem.backend_api.entity.MedicineHistory;
import com.op1m.medrem.backend_api.entity.Reminder;
import com.op1m.medrem.backend_api.entity.User;
import com.op1m.medrem.backend_api.entity.enums.MedicineStatus;
import com.op1m.medrem.backend_api.repository.MedicineHistoryRepository;
import com.op1m.medrem.backend_api.repository.ReminderRepository;
import com.op1m.medrem.backend_api.service.MedicineHistoryService;
import com.op1m.medrem.backend_api.service.NotificationService;
import com.op1m.medrem.backend_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Service
public class MedicineHistoryServiceImpl implements MedicineHistoryService {

    @Autowired
    private MedicineHistoryRepository historyRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public MedicineHistory createScheduleDose(Long reminderId, OffsetDateTime scheduledTime) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found: " + reminderId));

        MedicineHistory history = new MedicineHistory();
        history.setReminder(reminder);
        history.setScheduledTime(scheduledTime != null ? scheduledTime : OffsetDateTime.now(ZoneOffset.UTC));
        history.setStatus(MedicineStatus.PENDING);

        return historyRepository.save(history);
    }

    @Override
    @Transactional
    public MedicineHistory markAsTaken(Long historyId, String notes) {
        MedicineHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("History not found: " + historyId));

        history.markAsTaken();
        if (notes != null) {
            history.setNotes(notes);
        }

        return historyRepository.save(history);
    }

    @Override
    @Transactional
    public MedicineHistory markAsSkipped(Long historyId) {
        MedicineHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("History not found: " + historyId));

        history.markAsSkipped();
        return historyRepository.save(history);
    }

    @Override
    public List<MedicineHistory> getUserMedicineHistory(Long userId) {
        User user = userService.findById(userId);
        if (user == null) throw new RuntimeException("User not found");
        return historyRepository.findByReminderUserOrderByScheduledTimeDesc(user);
    }

    @Override
    public List<MedicineHistory> getMedicineHistoryByStatus(Long userId, MedicineStatus status) {
        if (userId == null) return historyRepository.findByStatus(status);
        User user = userService.findById(userId);
        return historyRepository.findByReminderUserAndStatusOrderByScheduledTimeDesc(user, status);
    }

    @Override
    public List<MedicineHistory> getHistoryByPeriod(Long userId, OffsetDateTime start, OffsetDateTime end) {
        User user = userService.findById(userId);
        return historyRepository.findByUserAndPeriod(user, start, end);
    }

    @Override
    @Transactional
    public void markReminderAsTakenByBot(Long reminderId, Long chatId) {
        Reminder reminder = reminderRepository.findById(reminderId).orElseThrow();

        List<MedicineHistory> histories = historyRepository.findByReminderUserOrderByScheduledTimeDesc(reminder.getUser());

        MedicineHistory history = histories.stream()
                .filter(h -> h.getReminder().getId().equals(reminderId))
                .filter(h -> h.getStatus() == MedicineStatus.PENDING || h.getStatus() == MedicineStatus.POSTPONED)
                .max(Comparator.comparing(MedicineHistory::getScheduledTime))
                .orElseThrow(() -> new RuntimeException("No active history found for this course"));

        history.markAsTaken();
        historyRepository.save(history);
    }

    @Override
    @Transactional
    public void markReminderAsSkippedByBot(Long reminderId, Long chatId) {
        Reminder reminder = reminderRepository.findById(reminderId).orElseThrow();

        List<MedicineHistory> histories = historyRepository.findByReminderUserOrderByScheduledTimeDesc(reminder.getUser());

        MedicineHistory history = histories.stream()
                .filter(h -> h.getReminder().getId().equals(reminderId))
                .filter(h -> h.getStatus() == MedicineStatus.PENDING || h.getStatus() == MedicineStatus.POSTPONED)
                .max(Comparator.comparing(MedicineHistory::getScheduledTime))
                .orElseThrow(() -> new RuntimeException("No active history found for this course"));

        history.markAsSkipped();
        historyRepository.save(history);
    }

    @Override
    @Transactional
    public MedicineHistory postponeReminder(Long reminderId, Long chatId, int minutes) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found: " + reminderId));

        if (reminder.getUser().getTelegramChatId() == null
                || !reminder.getUser().getTelegramChatId().equals(chatId)) {
            throw new RuntimeException("Chat ID does not match reminder owner");
        }

        // ✅ Всегда берём текущее время в UTC
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime newTime = now.plusMinutes(minutes);

        // Ищем последнюю активную историю (PENDING или POSTPONED)
        List<MedicineHistory> histories = historyRepository.findByReminderUserOrderByScheduledTimeDesc(reminder.getUser());
        MedicineHistory history = histories.stream()
                .filter(h -> h.getReminder().getId().equals(reminderId))
                .filter(h -> h.getStatus() == MedicineStatus.PENDING || h.getStatus() == MedicineStatus.POSTPONED)
                .max(Comparator.comparing(MedicineHistory::getScheduledTime))
                .orElse(null);

        if (history == null) {
            history = new MedicineHistory(reminder, newTime);
        } else {
            // Если есть существующая история, обновляем её время и статус
            history.setScheduledTime(newTime);
        }

        history.setStatus(MedicineStatus.POSTPONED);
        history.setNotes("Отложено на " + minutes + " минут");

        return historyRepository.save(history);
    }

    @Override
    @Transactional
    public void checkPostponedReminders() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<MedicineHistory> postponedHistories = historyRepository.findByStatusAndScheduledTimeBefore(
                MedicineStatus.POSTPONED, now
        );

        for (MedicineHistory history : postponedHistories) {
            Reminder reminder = history.getReminder();
            if (reminder == null) continue;

            notificationService.notifyUser(reminder);
            history.setStatus(MedicineStatus.PENDING);
            historyRepository.save(history);
        }
    }

    @Override
    @Transactional
    public void checkAndMarkMissedDoses() {
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10);
        List<MedicineHistory> pendingHistories =
                historyRepository.findByStatusAndScheduledTimeBefore(MedicineStatus.PENDING, threshold);

        for (MedicineHistory history : pendingHistories) {
            history.markAsMissed();
        }

        historyRepository.saveAll(pendingHistories);
    }

    @Override
    public MedicineHistory findById(Long historyId) {
        return historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("History not found: " + historyId));
    }

    @Override
    public MedicineHistory save(MedicineHistory history) {
        return historyRepository.save(history);
    }
}