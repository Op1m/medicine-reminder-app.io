package com.op1m.medrem.backend_api.controller;

import com.op1m.medrem.backend_api.entity.Course;
import com.op1m.medrem.backend_api.entity.CourseMedication;
import com.op1m.medrem.backend_api.entity.enums.CourseScheduleType;
import com.op1m.medrem.backend_api.entity.enums.MealMode;
import com.op1m.medrem.backend_api.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Course>> getUserCourses(@PathVariable Long userId) {
        return ResponseEntity.ok(courseService.getUserCourses(userId));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<Course> getCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.findById(courseId));
    }

    @DeleteMapping("/{courseId}")
public ResponseEntity<Map<String, Object>> deleteCourse(@PathVariable Long courseId) {
    courseService.deleteCourse(courseId);
    Map<String, Object> response = new HashMap<>();
    response.put("courseId", courseId);
    response.put("deleted", true);
    return ResponseEntity.ok(response);
}

@PutMapping("/{courseId}")
public ResponseEntity<Course> updateCourse(@PathVariable Long courseId,
                                           @RequestBody UpdateCourseRequest request) {
    Course course = courseService.updateCourse(courseId, request.getName(),
            request.getStartDate(), request.getEndDate());
    return ResponseEntity.ok(course);
}


    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody CreateCourseRequest request) {
        Course course = courseService.createCourse(
                request.getUserId(),
                request.getName(),
                request.getStartDate(),
                request.getEndDate()
        );
        return ResponseEntity.ok(course);
    }

    @PostMapping("/{courseId}/medications")
    public ResponseEntity<CourseMedication> addMedication(
            @PathVariable Long courseId,
            @RequestBody CreateCourseMedicationRequest request
    ) {
        CourseMedication medication = new CourseMedication(
                request.getMedicineName(),
                request.getDosage(),
                request.getDescription(),
                request.getInstructions(),
                request.getMealMode(),
                request.getTimeOfDay(),
                request.getScheduleType(),
                request.getIntervalDays()
        );

        return ResponseEntity.ok(courseService.addMedicationToCourse(courseId, medication));
    }

    @PostMapping("/{courseId}/generate-reminders")
    public ResponseEntity<Map<String, Object>> generateReminders(@PathVariable Long courseId) {
        int created = courseService.generateRemindersForCourse(courseId);
        Map<String, Object> response = new HashMap<>();
        response.put("courseId", courseId);
        response.put("createdReminders", created);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{courseId}/medications/{medicationId}")
public ResponseEntity<Map<String, Object>> deleteMedication(
        @PathVariable Long courseId,
        @PathVariable Long medicationId) {

    courseService.deleteMedicationFromCourse(courseId, medicationId);

    Map<String, Object> response = new HashMap<>();
    response.put("courseId", courseId);
    response.put("medicationId", medicationId);
    response.put("deleted", true);
    return ResponseEntity.ok(response);
}

// Обновление препарата в курсе
@PutMapping("/{courseId}/medications/{medicationId}")
public ResponseEntity<CourseMedication> updateMedication(
        @PathVariable Long courseId,
        @PathVariable Long medicationId,
        @RequestBody CreateCourseMedicationRequest request) {

    CourseMedication medication = courseService.updateMedicationInCourse(
            courseId, medicationId, request);

    return ResponseEntity.ok(medication);
}

    @PostMapping("/{courseId}/regenerate-future-reminders")
    public ResponseEntity<Map<String, Object>> regenerateFutureReminders(@PathVariable Long courseId) {
        int created = courseService.regenerateFutureReminders(courseId);
        Map<String, Object> response = new HashMap<>();
        response.put("courseId", courseId);
        response.put("createdReminders", created);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{courseId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateCourse(@PathVariable Long courseId) {
        Course course = courseService.deactivateCourse(courseId);
        Map<String, Object> response = new HashMap<>();
        response.put("courseId", course.getId());
        response.put("active", course.getActive());
        return ResponseEntity.ok(response);
    }

    public static class CreateCourseRequest {
        private Long userId;
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    public static class CreateCourseMedicationRequest {
        private String medicineName;
        private String dosage;
        private String description;
        private String instructions;
        private MealMode mealMode;
        private LocalTime timeOfDay;
        private CourseScheduleType scheduleType;
        private Integer intervalDays;

        public String getMedicineName() { return medicineName; }
        public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getInstructions() { return instructions; }
        public void setInstructions(String instructions) { this.instructions = instructions; }

        public MealMode getMealMode() { return mealMode; }
        public void setMealMode(MealMode mealMode) { this.mealMode = mealMode; }

        public LocalTime getTimeOfDay() { return timeOfDay; }
        public void setTimeOfDay(LocalTime timeOfDay) { this.timeOfDay = timeOfDay; }

        public CourseScheduleType getScheduleType() { return scheduleType; }
        public void setScheduleType(CourseScheduleType scheduleType) { this.scheduleType = scheduleType; }

        public Integer getIntervalDays() { return intervalDays; }
        public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }
    }

    public static class UpdateCourseRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
}