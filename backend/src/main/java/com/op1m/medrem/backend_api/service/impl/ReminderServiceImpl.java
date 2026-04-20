package com.op1m.medrem.backend_api.service.impl;

import com.op1m.medrem.backend_api.entity.CourseMedication;
import com.op1m.medrem.backend_api.entity.Reminder;
import com.op1m.medrem.backend_api.entity.User;
import com.op1m.medrem.backend_api.entity.Medicine;
import com.op1m.medrem.backend_api.repository.MedicineHistoryRepository;
import com.op1m.medrem.backend_api.repository.MedicineRepository;
import com.op1m.medrem.backend_api.repository.ReminderRepository;
import com.op1m.medrem.backend_api.repository.UserRepository;
import com.op1m.medrem.backend_api.service.ReminderService;
import com.op1m.medrem.backend_api.service.UserService;
import com.op1m.medrem.backend_api.service.MedicineService;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ReminderServiceImpl implements ReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderServiceImpl.class);

    private final ReminderRepository reminderRepository;
    private final UserService userService;
    private final MedicineService medicineService;
    private final MedicineHistoryRepository medicineHistoryRepository;

    @Autowired
    public ReminderServiceImpl(ReminderRepository reminderRepository,
                               UserService userService,
                               MedicineService medicineService,
                               MedicineHistoryRepository medicineHistoryRepository) {
        this.reminderRepository = reminderRepository;
        this.userService = userService;
        this.medicineService = medicineService;
        this.medicineHistoryRepository = medicineHistoryRepository;
    }

    @Override
    @CacheEvict(cacheNames = "remindersByUser", allEntries = true)
    public Reminder createReminder(Long userId, Long medicineId, LocalTime reminderTime, String daysOfWeek) {
        logger.info("Создание напоминания: user={}, medicine={}, time={}", userId, medicineId, reminderTime);
        User user = userService.findById(userId);
        if (user == null) throw new RuntimeException("Пользователь не найден");
        Medicine medicine = medicineService.findById(medicineId);
        if (medicine == null) throw new RuntimeException("Лекарство не найдено");
        Reminder reminder = new Reminder(user, medicine, reminderTime);
        if (daysOfWeek != null) reminder.setDaysOfWeek(daysOfWeek);
        Reminder savedReminder = reminderRepository.save(reminder);
        logger.info("Напоминание создано: id={}", savedReminder.getId());
        return savedReminder;
    }

    @Override
    @Transactional
public Reminder createCourseReminder(Long userId, Long medicineId, LocalTime reminderTime,
                                     LocalDate specificDate, CourseMedication courseMedication) {
    User user = userService.findById(userId);

    Medicine medicine = medicineService.findById(medicineId);

    Reminder reminder = new Reminder();
    reminder.setUser(user);
    reminder.setMedicine(medicine);
    reminder.setReminderTime(reminderTime);
    reminder.setActive(true);

    // 👇 ВАЖНО! Эти поля делают напоминание курсовым
    reminder.setSpecificDate(specificDate);        // ← ДОЛЖНО БЫТЬ!
    reminder.setCourseMedication(courseMedication); // ← ДОЛЖНО БЫТЬ!
    reminder.setDaysOfWeek(null);                  // ← ДОЛЖНО БЫТЬ null!

    return reminderRepository.save(reminder);
}

    @Override
    @Transactional(readOnly = true)
    public List<Reminder> getUserReminders(Long userId) {
        logger.debug("Получение напоминаний для user={}", userId);
        User user = userService.findById(userId);
        if (user == null) throw new RuntimeException("Пользователь не найден");
        List<Reminder> reminders = reminderRepository.findByUser(user);

        for (Reminder reminder : reminders) {
            Hibernate.initialize(reminder.getUser());
            Hibernate.initialize(reminder.getMedicine());
            if (reminder.getMedicine() != null) {
                Hibernate.initialize(reminder.getMedicine().getCategories());
            }
        }

        logger.debug("Найдено {} напоминаний", reminders.size());
        return reminders;
    }

    @Override
    public Reminder findById(Long reminderId) {
        logger.debug("Поиск напоминания id={}", reminderId);
        return reminderRepository.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Напоминание не найдено"));
    }

    @Override
    @Cacheable(cacheNames = "remindersByUser", key = "#userId")
    public List<Reminder> getUserActiveReminders(Long userId) {
        logger.debug("Получение активных напоминаний для user={}", userId);
        User user = userService.findById(userId);
        if (user == null) throw new RuntimeException("Пользователь не найден");
        return reminderRepository.findByUserAndIsActiveTrue(user);
    }

    @Override
    @Cacheable(cacheNames = "remindersAll")
    public List<Reminder> getAllActiveReminders() {
        logger.debug("Получение всех активных напоминаний");
        return reminderRepository.findByIsActiveTrue();
    }

    @Override
    public List<Reminder> getDueReminders() {
        logger.debug("Поиск напоминаний для отправки");
        List<Reminder> dueReminders = new ArrayList<>();
        List<Reminder> activeReminders = reminderRepository.findAllActiveWithUserAndMedicine();
        for (Reminder reminder : activeReminders) {
            if (shouldNotifyNow(reminder)) {
                dueReminders.add(reminder);
                logger.debug("Найдено для отправки: id={}", reminder.getId());
            }
        }
        logger.debug("Всего для отправки: {}", dueReminders.size());
        return dueReminders;
    }

    @Override
    @CacheEvict(cacheNames = {"remindersByUser", "remindersAll"}, allEntries = true)
    public Reminder toggleReminder(Long reminderId, Boolean isActive) {
        logger.debug("Toggle reminder {} -> {}", reminderId, isActive);
        Reminder reminder = reminderRepository.findById(reminderId).orElse(null);
        if (reminder == null) return null;
        reminder.setActive(isActive);
        Reminder updated = reminderRepository.save(reminder);
        logger.info("Reminder {} active -> {}", updated.getId(), isActive);
        return updated;
    }

    @Override
    @CacheEvict(cacheNames = {"remindersByUser", "remindersAll"}, allEntries = true)
    public Reminder updateReminderTime(Long reminderId, LocalTime newTime) {
        logger.debug("Update reminder time {} -> {}", reminderId, newTime);
        Reminder reminder = reminderRepository.findById(reminderId).orElse(null);
        if (reminder == null) return null;
        reminder.setReminderTime(newTime);
        Reminder updated = reminderRepository.save(reminder);
        logger.info("Reminder {} time updated", updated.getId());
        return updated;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"remindersByUser", "remindersAll"}, allEntries = true)
    public boolean deleteReminder(Long reminderId) {
        logger.debug("Delete reminder {}", reminderId);
        if (!reminderRepository.existsById(reminderId)) {
            logger.warn("Reminder {} not found for deletion", reminderId);
            return false;
        }
        try {
            medicineHistoryRepository.deleteByReminderId(reminderId);
            reminderRepository.deleteById(reminderId);
            logger.info("Reminder {} and related history deleted", reminderId);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting reminder history: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean shouldNotifyNow(Reminder reminder) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (reminder.getReminderTime() == null) {
            return false;
        }

        LocalTime reminderTime = reminder.getReminderTime().withSecond(0).withNano(0);
        LocalTime currentTime = now.toLocalTime().withSecond(0).withNano(0);

        boolean timeMatches = reminderTime.equals(currentTime);

        if (reminder.getSpecificDate() != null) {
            return timeMatches && reminder.getSpecificDate().equals(now.toLocalDate());
        }

        String daysOfWeek = reminder.getDaysOfWeek();
        if (daysOfWeek == null || "everyday".equals(daysOfWeek)) {
            return timeMatches;
        }

        int currentDay = now.getDayOfWeek().getValue();
        return timeMatches && daysOfWeek.contains(String.valueOf(currentDay));
    }

private boolean checkDayOfWeek(Reminder reminder, OffsetDateTime now) {
    String daysOfWeek = reminder.getDaysOfWeek();

    if (daysOfWeek == null || daysOfWeek.equals("everyday")) {
        return true;
    }

    int currentDay = now.getDayOfWeek().getValue();
    return daysOfWeek.contains(String.valueOf(currentDay));
}

    @Override
    @CacheEvict(cacheNames = {"remindersByUser", "remindersAll"}, allEntries = true)
    public Reminder updateReminder(Long reminderId, Long medicineId, LocalTime reminderTime, String daysOfWeek) {
        logger.debug("Update reminder id={}", reminderId);
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Напоминание не найдено"));
        Medicine medicine = medicineService.findById(medicineId);
        if (medicine == null) throw new RuntimeException("Лекарство не найдено");
        reminder.setMedicine(medicine);
        reminder.setReminderTime(reminderTime);
        if (daysOfWeek != null) reminder.setDaysOfWeek(daysOfWeek);
        Reminder updated = reminderRepository.save(reminder);
        logger.info("Reminder {} updated", updated.getId());
        return updated;
    }



    @Async
    public CompletableFuture<List<Reminder>> getUserRemindersAsync(Long userId) {
        return CompletableFuture.completedFuture(getUserReminders(userId));
    }
}