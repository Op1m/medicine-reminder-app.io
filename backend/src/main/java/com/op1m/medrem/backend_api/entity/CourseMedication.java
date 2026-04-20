package com.op1m.medrem.backend_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.op1m.medrem.backend_api.entity.enums.CourseScheduleType;
import com.op1m.medrem.backend_api.entity.enums.MealMode;
import jakarta.persistence.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "course_medications")
public class CourseMedication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "medicine_name", nullable = false)
    private String medicineName;

    @Column(name = "dosage")
    private String dosage;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "instructions", length = 1000)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_mode", nullable = false)
    private MealMode mealMode = MealMode.ANYTIME;

    @Column(name = "time_of_day", nullable = false)
    private LocalTime timeOfDay;

    @Transient
    private Long medicineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private CourseScheduleType scheduleType = CourseScheduleType.EVERY_DAY;

    @Column(name = "interval_days")
    private Integer intervalDays = 1;

    @Column(name = "generated_medicine_id")
    private Long generatedMedicineId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public CourseMedication() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public CourseMedication(String medicineName, String dosage, String description, String instructions,
                            MealMode mealMode, LocalTime timeOfDay,
                            CourseScheduleType scheduleType, Integer intervalDays) {
        this();
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.description = description;
        this.instructions = instructions;
        this.mealMode = mealMode != null ? mealMode : MealMode.ANYTIME;
        this.timeOfDay = timeOfDay;
        this.scheduleType = scheduleType != null ? scheduleType : CourseScheduleType.EVERY_DAY;
        this.intervalDays = intervalDays != null ? intervalDays : 1;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getDosage() {
        return dosage;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        return instructions;
    }

    public MealMode getMealMode() {
        return mealMode;
    }

    public LocalTime getTimeOfDay() {
        return timeOfDay;
    }

    public CourseScheduleType getScheduleType() {
        return scheduleType;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public Long getGeneratedMedicineId() {
        return generatedMedicineId;
    }

    public Boolean getActive() {
        return isActive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCourse(Course course) {
        this.course = course;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setMealMode(MealMode mealMode) {
        this.mealMode = mealMode;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setTimeOfDay(LocalTime timeOfDay) {
        this.timeOfDay = timeOfDay;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setScheduleType(CourseScheduleType scheduleType) {
        this.scheduleType = scheduleType;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setGeneratedMedicineId(Long generatedMedicineId) {
        this.generatedMedicineId = generatedMedicineId;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setActive(Boolean active) {
        isActive = active;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}