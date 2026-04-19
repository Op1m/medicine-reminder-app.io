package com.op1m.medrem.backend_api.service.impl;

import com.op1m.medrem.backend_api.entity.*;
import com.op1m.medrem.backend_api.entity.enums.MedicineStatus;
import com.op1m.medrem.backend_api.repository.MedicineHistoryRepository;
import com.op1m.medrem.backend_api.repository.ReminderRepository;
import com.op1m.medrem.backend_api.service.MedicineHistoryService;
import com.op1m.medrem.backend_api.service.NotificationService;
import com.op1m.medrem.backend_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    Reminder reminder = history.getReminder();
    LocalDate date = history.getScheduledTime().toLocalDate();
    OffsetDateTime startOfDay = date.atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

    List<MedicineHistory> todayHistories = historyRepository.findByUserAndPeriod(reminder.getUser(), startOfDay, endOfDay);
    for (MedicineHistory h : todayHistories) {
        if (h.getReminder().getId().equals(reminder.getId()) && h.getStatus() == MedicineStatus.POSTPONED && !h.getId().equals(historyId)) {
            historyRepository.delete(h);
        }
    }

    return historyRepository.save(history);
}

@Override
@Transactional
public MedicineHistory markAsSkipped(Long historyId) {
    MedicineHistory history = historyRepository.findWithReminderAndRelationsById(historyId);
    if (history == null) {
        throw new RuntimeException("MedicineHistory not found: " + historyId);
    }
    history.markAsSkipped();


    Reminder reminder = history.getReminder();
    LocalDate date = history.getScheduledTime().toLocalDate();
    OffsetDateTime startOfDay = date.atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

    List<MedicineHistory> todayHistories = historyRepository.findByUserAndPeriod(reminder.getUser(), startOfDay, endOfDay);
    for (MedicineHistory h : todayHistories) {
        if (h.getReminder().getId().equals(reminder.getId()) && h.getStatus() == MedicineStatus.POSTPONED && !h.getId().equals(historyId)) {
            historyRepository.delete(h);
        }
    }

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
        User user = userService.findById(userId);
        if (user == null) throw new RuntimeException("User not found");
        return historyRepository.findByReminderUserAndStatusOrderByScheduledTimeDesc(user, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineHistory> getHistoryByPeriod(Long userId, OffsetDateTime start, OffsetDateTime end) {
        User user = userService.findById(userId);
        if (user == null) throw new RuntimeException("User not found");
        return historyRepository.findByUserAndPeriod(user, start, end);
    }

    @Override
    @Transactional
    public void checkAndMarkMissedDoses() {
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10);
        List<MedicineHistory> pendingHistories = historyRepository.findByStatusAndScheduledTimeBefore(
                MedicineStatus.PENDING, threshold
        );
        for (MedicineHistory history : pendingHistories) {
            history.markAsMissed();
        }
        historyRepository.saveAll(pendingHistories);
    }

    @Override
    // MedicineHistoryServiceImpl.postponeReminder(...)
@Transactional
public MedicineHistory postponeReminder(Long reminderId, Long chatId, int minutes) {
    Reminder reminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new RuntimeException("Reminder not found: " + reminderId));

    if (!reminder.getUser().getTelegramChatId().equals(chatId)) {
        throw new RuntimeException("Chat ID does not match reminder owner");
    }

    OffsetDateTime newTime = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(minutes);

    OffsetDateTime startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

    List<MedicineHistory> todayHistories =
            historyRepository.findByUserAndPeriod(reminder.getUser(), startOfDay, endOfDay);

    MedicineHistory history = todayHistories.stream()
            .filter(h -> h.getReminder().getId().equals(reminderId))
            .max(Comparator.comparing(MedicineHistory::getCreatedAt))
            .orElse(new MedicineHistory(reminder, newTime));

    history.setScheduledTime(newTime);
    history.setStatus(MedicineStatus.POSTPONED);
    history.setNotes("Отложено на " + minutes + " минут");

    return historyRepository.save(history);
}

    @Override
@Transactional
public void markReminderAsTakenByBot(Long reminderId, Long chatId) {
    Reminder reminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new RuntimeException("Reminder not found: " + reminderId));

    if (!reminder.getUser().getTelegramChatId().equals(chatId)) {
        throw new RuntimeException("Chat ID does not match reminder owner");
    }

    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    OffsetDateTime startOfDay = today.atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

    List<MedicineHistory> histories = historyRepository.findByUserAndPeriod(
            reminder.getUser(), startOfDay, endOfDay);

    MedicineHistory history = histories.stream()
            .filter(h -> h.getReminder().getId().equals(reminderId) &&
                        (h.getStatus() == MedicineStatus.PENDING || h.getStatus() == MedicineStatus.POSTPONED))
            .findFirst()
            .orElse(null);

    if (history == null) {
        OffsetDateTime scheduledTime = today.atTime(reminder.getReminderTime()).atOffset(ZoneOffset.UTC);
        history = new MedicineHistory(reminder, scheduledTime);
        history = historyRepository.save(history);
    }

    history.markAsTaken();
    historyRepository.save(history);

    for (MedicineHistory h : histories) {
        if (h.getReminder().getId().equals(reminderId) && h.getStatus() == MedicineStatus.POSTPONED && !h.getId().equals(history.getId())) {
            historyRepository.delete(h);
        }
    }

    System.out.println("✅ Напоминание " + reminderId + " отмечено как принято через бота");
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

    @Override
    @Transactional
    public void markReminderAsSkippedByBot(Long reminderId, Long chatId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found: " + reminderId));

        if (!reminder.getUser().getTelegramChatId().equals(chatId)) {
            throw new RuntimeException("Chat ID does not match reminder owner");
        }

        OffsetDateTime startOfDay = OffsetDateTime.now(ZoneOffset.UTC).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        List<MedicineHistory> histories = historyRepository.findByUserAndPeriod(
                reminder.getUser(), startOfDay, endOfDay);

        MedicineHistory history = histories.stream()
                .filter(h -> h.getReminder().getId().equals(reminderId))
                .findFirst()
                .orElse(null);

        if (history == null) {
            OffsetDateTime scheduledTime = startOfDay.withHour(reminder.getReminderTime().getHour())
                    .withMinute(reminder.getReminderTime().getMinute());
            history = new MedicineHistory(reminder, scheduledTime);
            history = historyRepository.save(history);
        }

        history.markAsSkipped();
        historyRepository.save(history);

        System.out.println("✅ Напоминание " + reminderId + " отмечено как пропущено через бота");
    }

@Override
@Transactional
public void checkPostponedReminders() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    // Ищем именно POSTPONED, у которых время уже наступило
    List<MedicineHistory> postponedHistories =
            historyRepository.findByStatusAndScheduledTimeBefore(
                    MedicineStatus.POSTPONED, now
            );

    for (MedicineHistory history : postponedHistories) {
        Reminder reminder = history.getReminder();
        if (reminder == null) {
            continue;
        }

        // Отправляем повторное напоминание с кнопками через NotificationService
        notificationService.notifyUser(reminder);

        // ВАЖНО:
        // после повторной отправки переводим запись в PENDING,
        // чтобы через 10 минут checkAndMarkMissedDoses() пометил её как missed,
        // если пользователь ничего не нажал.
        history.setStatus(MedicineStatus.PENDING);
        history.setScheduledTime(now);
        historyRepository.save(history);
    }
}
}