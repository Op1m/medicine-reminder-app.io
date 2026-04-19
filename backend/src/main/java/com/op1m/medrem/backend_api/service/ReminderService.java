package com.op1m.medrem.backend_api.service;

import com.op1m.medrem.backend_api.entity.CourseMedication;
import com.op1m.medrem.backend_api.entity.Reminder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReminderService {
    Reminder createReminder(Long userId, Long medicineId, LocalTime reminderTime, String daysOfWeek);
    List<Reminder> getUserReminders(Long userId);
    Reminder findById(Long reminderId);
    List<Reminder> getUserActiveReminders(Long userId);
    List<Reminder> getAllActiveReminders();
    List<Reminder> getDueReminders();
    Reminder toggleReminder(Long reminderId, Boolean isActive);
    Reminder updateReminderTime(Long reminderId, LocalTime newTime);
    boolean deleteReminder(Long reminderId);
    boolean shouldNotifyNow(Reminder reminder);
    Reminder updateReminder(Long reminderId, Long medicineId, LocalTime reminderTime, String daysOfWeek);
    Reminder createCourseReminder(Long userId, Long medicineId, LocalTime reminderTime, LocalDate specificDate, CourseMedication courseMedication);
}