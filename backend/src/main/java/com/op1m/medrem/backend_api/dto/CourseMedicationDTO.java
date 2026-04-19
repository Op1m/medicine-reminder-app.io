package com.op1m.medrem.backend_api.dto;

import java.time.LocalTime;

public class CourseMedicationDTO {
    private Long id;
    private Long courseId;
    private String medicineName;
    private String dosage;
    private String description;
    private String instructions;
    private String mealMode;
    private LocalTime timeOfDay;
    private String scheduleType;
    private Integer intervalDays;
    private Long generatedMedicineId;
    private Boolean isActive;

    // Конструкторы
    public CourseMedicationDTO() {}

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getMealMode() { return mealMode; }
    public void setMealMode(String mealMode) { this.mealMode = mealMode; }

    public LocalTime getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(LocalTime timeOfDay) { this.timeOfDay = timeOfDay; }

    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    public Integer getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }

    public Long getGeneratedMedicineId() { return generatedMedicineId; }
    public void setGeneratedMedicineId(Long generatedMedicineId) { this.generatedMedicineId = generatedMedicineId; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}