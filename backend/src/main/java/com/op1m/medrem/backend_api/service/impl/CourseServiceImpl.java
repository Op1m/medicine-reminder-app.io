package com.op1m.medrem.backend_api.service.impl;

import com.op1m.medrem.backend_api.entity.*;
import com.op1m.medrem.backend_api.entity.enums.CourseScheduleType;
import com.op1m.medrem.backend_api.entity.enums.MealMode;
import com.op1m.medrem.backend_api.repository.CourseMedicationRepository;
import com.op1m.medrem.backend_api.repository.CourseRepository;
import com.op1m.medrem.backend_api.repository.ReminderRepository;
import com.op1m.medrem.backend_api.service.CourseService;
import com.op1m.medrem.backend_api.service.MedicineService;
import com.op1m.medrem.backend_api.service.ReminderService;
import com.op1m.medrem.backend_api.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CourseServiceImpl implements CourseService {

    private static final Pattern DATE_KEY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseMedicationRepository courseMedicationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Override
    @Transactional
    public Course createCourse(Long userId, String name, LocalDate startDate, LocalDate endDate) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found: " + userId);
        }
        if (startDate == null || endDate == null) {
            throw new RuntimeException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("endDate must be on or after startDate");
        }

        Course course = new Course(user, name, startDate, endDate);
        course.setActive(true);
        return courseRepository.save(course);
    }

    @Override
    public List<Course> getUserCourses(Long userId) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found: " + userId);
        }
        return courseRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public Course findById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));
    }

    @Override
    @Transactional
    public CourseMedication addMedicationToCourse(Long courseId, CourseMedication medication) {
        Course course = findById(courseId);
        if (medication == null) {
            throw new RuntimeException("Medication payload is required");
        }
        if (medication.getMedicineName() == null || medication.getMedicineName().isBlank()) {
            throw new RuntimeException("medicineName is required");
        }
        if (medication.getTimeOfDay() == null) {
            throw new RuntimeException("timeOfDay is required");
        }

        medication.setCourse(course);
        medication.setActive(true);

        if (medication.getMealMode() == null) {
            medication.setMealMode(MealMode.ANYTIME);
        }
        if (medication.getScheduleType() == null) {
            medication.setScheduleType(CourseScheduleType.EVERY_DAY);
        }
        if (medication.getIntervalDays() == null || medication.getIntervalDays() < 1) {
            medication.setIntervalDays(1);
        }

        return courseMedicationRepository.save(medication);
    }

    @Override
    @Transactional
    public int generateRemindersForCourse(Long courseId) {
        Course course = findById(courseId);
        if (!Boolean.TRUE.equals(course.getActive())) {
            throw new RuntimeException("Course is inactive: " + courseId);
        }

        return generateRemindersInternal(course, false);
    }

    @Override
    @Transactional
    public int regenerateFutureReminders(Long courseId) {
        Course course = findById(courseId);
        if (!Boolean.TRUE.equals(course.getActive())) {
            throw new RuntimeException("Course is inactive: " + courseId);
        }

        deleteFutureGeneratedReminders(course);
        return generateRemindersInternal(course, true);
    }

@Override
@Transactional
public void deleteCourse(Long courseId) {
    Course course = findById(courseId);

    // Получаем все медикаменты курса
    List<CourseMedication> medications = courseMedicationRepository.findByCourseIdOrderByIdAsc(courseId);

    for (CourseMedication med : medications) {
        // Удаляем все напоминания, связанные с этим медикаментом курса
        List<Reminder> reminders = reminderRepository.findByCourseMedicationId(med.getId());
        reminderRepository.deleteAll(reminders);

        // Деактивируем сгенерированный medicine (вместо удаления)
        if (med.getGeneratedMedicineId() != null) {
            medicineService.deactivateMedicine(med.getGeneratedMedicineId());
        }
    }

    // Удаляем все медикаменты курса
    courseMedicationRepository.deleteAll(medications);

    // Удаляем сам курс
    courseRepository.delete(course);
}

@Override
@Transactional
public Course updateCourse(Long courseId, String name, LocalDate startDate, LocalDate endDate) {
    Course course = findById(courseId);
    if (name != null && !name.isBlank()) {
        course.setName(name);
    }
    if (startDate != null) {
        course.setStartDate(startDate);
    }
    if (endDate != null) {
        course.setEndDate(endDate);
    }
    // После обновления дат нужно перегенерировать напоминания (фронт это делает отдельно)
    return courseRepository.save(course);
}

@Override
@Transactional
public Course deactivateCourse(Long courseId) {
    Course course = findById(courseId);
    course.setActive(false);
    courseRepository.save(course);

    List<CourseMedication> meds = courseMedicationRepository.findByCourseIdOrderByIdAsc(courseId);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    LocalDate today = now.toLocalDate();

    for (CourseMedication med : meds) {
        med.setActive(false);
        courseMedicationRepository.save(med);

        if (med.getGeneratedMedicineId() != null) {
            Medicine medicine = medicineService.findById(med.getGeneratedMedicineId());
            if (medicine != null && medicine.isActive()) {
                medicineService.deactivateMedicine(med.getGeneratedMedicineId());
            }
        }

        // Деактивируем будущие напоминания этого медикамента
        List<Reminder> reminders = reminderRepository.findByCourseMedicationId(med.getId());
        for (Reminder reminder : reminders) {
            LocalDate specificDate = reminder.getSpecificDate();
            LocalTime reminderTime = reminder.getReminderTime();

            boolean isFuture = specificDate != null && reminderTime != null &&
                    (specificDate.isAfter(today) ||
                     (specificDate.isEqual(today) && reminderTime.isAfter(now.toLocalTime())));

            if (isFuture) {
                reminder.setActive(false);
                reminderRepository.save(reminder);
            }
        }
    }

    return course;
}

private int generateRemindersInternal(Course course, boolean futureOnly) {
    List<CourseMedication> medications = courseMedicationRepository.findByCourseIdOrderByIdAsc(course.getId());
    if (medications.isEmpty()) {
        System.out.println("No medications found for course " + course.getId());
        return 0;
    }

    // Загружаем только напоминания этого курса
    List<Reminder> existingReminders = reminderRepository.findByCourseMedication_Course_Id(course.getId());

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    LocalDate today = now.toLocalDate();
    int created = 0;

    for (CourseMedication medication : medications) {
        System.out.println("Processing medication: " + medication.getId() + " " + medication.getMedicineName());
        if (!Boolean.TRUE.equals(medication.getActive())) {
            continue;
        }

        Medicine medicine = resolveMedicineForCourseMedication(medication);
        if (medicine == null) {
            System.out.println("Processing medication: " + medication.getId() + " " + medication.getMedicineName());
            continue;
        }

        LocalDate current = course.getStartDate();
        while (!current.isAfter(course.getEndDate())) {
            boolean isFuture = current.isAfter(today) ||
                    (current.isEqual(today) && medication.getTimeOfDay().isAfter(now.toLocalTime()));

            if (!futureOnly || isFuture) {
                if (matchesSchedule(course.getStartDate(), current, medication)) {
                    System.out.println("Date " + current + " matches schedule");
                    final LocalDate currentDate = current;

                    // Безопасная проверка существования
                    boolean alreadyExists = false;
                    for (Reminder r : existingReminders) {
                        if (r.getMedicine() != null &&
                            r.getMedicine().getId().equals(medicine.getId()) &&
                            r.getReminderTime() != null &&
                            r.getReminderTime().equals(medication.getTimeOfDay()) &&
                            currentDate.equals(r.getSpecificDate()) &&
                            r.getCourseMedication() != null &&
                            r.getCourseMedication().getId().equals(medication.getId()) &&
                            Boolean.TRUE.equals(r.getIsActive())) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        Reminder reminder = reminderService.createCourseReminder(
                                course.getUser().getId(),
                                medicine.getId(),
                                medication.getTimeOfDay(),
                                current,
                                medication
                        );

                        if (reminder != null) {
                            created++;
                            existingReminders.add(reminder);
                        }
                    }
                }
            }
            current = current.plusDays(1);
        }
    }
    return created;
}

    private Medicine resolveMedicineForCourseMedication(CourseMedication medication) {
        if (medication.getGeneratedMedicineId() != null) {
            Medicine existing = medicineService.findById(medication.getGeneratedMedicineId());
            if (existing != null) {
                return existing;
            }
        }

        String instructions = buildInstructions(medication);
        Medicine medicine = medicineService.createMedicine(
                medication.getMedicineName(),
                medication.getDosage(),
                medication.getDescription(),
                instructions
        );

        medication.setGeneratedMedicineId(medicine.getId());
        courseMedicationRepository.save(medication);
        return medicine;
    }

    private String buildInstructions(CourseMedication medication) {
        List<String> parts = new ArrayList<>();
        if (medication.getInstructions() != null && !medication.getInstructions().isBlank()) {
            parts.add(medication.getInstructions().trim());
        }

        if (medication.getMealMode() != null) {
            parts.add("Приём: " + mealModeLabel(medication.getMealMode()));
        }

        return String.join(" | ", parts);
    }

    private String mealModeLabel(MealMode mode) {
        return switch (mode) {
            case BEFORE_MEAL -> "до еды";
            case DURING_MEAL -> "во время еды";
            case AFTER_MEAL -> "после еды";
            case ANYTIME -> "независимо от еды";
        };
    }

    private boolean matchesSchedule(LocalDate startDate, LocalDate currentDate, CourseMedication medication) {
        int step;
        if (medication.getScheduleType() == CourseScheduleType.EVERY_DAY) {
            step = 1;
        } else {
            step = medication.getIntervalDays() != null ? Math.max(1, medication.getIntervalDays()) : 1;
        }

        long diff = java.time.temporal.ChronoUnit.DAYS.between(startDate, currentDate);
        return diff >= 0 && diff % step == 0;
    }

private void deleteFutureGeneratedReminders(Course course) {
    List<CourseMedication> meds = courseMedicationRepository.findByCourseIdOrderByIdAsc(course.getId());
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    LocalDate today = now.toLocalDate();

    for (CourseMedication medication : meds) {
        // Находим все напоминания этого медикамента в курсе
        List<Reminder> reminders = reminderRepository.findByCourseMedicationId(medication.getId());

        for (Reminder reminder : reminders) {
            if (!Boolean.TRUE.equals(reminder.getIsActive())) {
                continue;
            }

            LocalDate specificDate = reminder.getSpecificDate();
            LocalTime reminderTime = reminder.getReminderTime();

            // Удаляем только будущие
            if (specificDate != null && reminderTime != null) {
                boolean isFuture = specificDate.isAfter(today) ||
                        (specificDate.isEqual(today) && reminderTime.isAfter(now.toLocalTime()));

                if (isFuture) {
                    reminderRepository.delete(reminder);
                }
            }
        }
    }
}

    private boolean isFutureOccurrence(String dateKey, LocalTime timeOfDay, OffsetDateTime now) {
        if (dateKey == null || !DATE_KEY.matcher(dateKey).matches() || timeOfDay == null) {
            return false;
        }

        LocalDate date = LocalDate.parse(dateKey);
        LocalDate today = now.toLocalDate();
        return date.isAfter(today) || (date.isEqual(today) && timeOfDay.isAfter(now.toLocalTime()));
    }
}