package com.op1m.medrem.backend_api.service;

import com.op1m.medrem.backend_api.entity.Course;
import com.op1m.medrem.backend_api.entity.CourseMedication;

import java.time.LocalDate;
import java.util.List;

public interface CourseService {
    Course createCourse(Long userId, String name, LocalDate startDate, LocalDate endDate);
    List<Course> getUserCourses(Long userId);
    Course findById(Long courseId);
    CourseMedication addMedicationToCourse(Long courseId, CourseMedication medication);
void deleteCourse(Long courseId);
Course updateCourse(Long courseId, String name, LocalDate startDate, LocalDate endDate);
    int generateRemindersForCourse(Long courseId);
    int regenerateFutureReminders(Long courseId);
    Course deactivateCourse(Long courseId);
}