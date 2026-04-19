package com.op1m.medrem.backend_api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public class ReminderDTO {
    private Long id;
    private UserDTO user;
    private MedicineDTO medicine;
    private LocalTime reminderTime;
    private Boolean isActive;
    private String daysOfWeek;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // 👇 НОВЫЕ ПОЛЯ ДЛЯ КУРСОВЫХ НАПОМИНАНИЙ
    private LocalDate specificDate;
    private Long courseMedicationId;
    private CourseMedicationDTO courseMedication;

    // Конструктор для обычных напоминаний
    public ReminderDTO(Long id, UserDTO user, MedicineDTO medicine, LocalTime reminderTime,
                       Boolean isActive, String daysOfWeek, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.medicine = medicine;
        this.reminderTime = reminderTime;
        this.isActive = isActive;
        this.daysOfWeek = daysOfWeek;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Пустой конструктор
    public ReminderDTO() {}

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }

    public MedicineDTO getMedicine() { return medicine; }
    public void setMedicine(MedicineDTO medicine) { this.medicine = medicine; }

    public LocalTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalTime reminderTime) { this.reminderTime = reminderTime; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    // 👇 ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ НОВЫХ ПОЛЕЙ
    public LocalDate getSpecificDate() { return specificDate; }
    public void setSpecificDate(LocalDate specificDate) { this.specificDate = specificDate; }

    public Long getCourseMedicationId() { return courseMedicationId; }
    public void setCourseMedicationId(Long courseMedicationId) { this.courseMedicationId = courseMedicationId; }

    public CourseMedicationDTO getCourseMedication() { return courseMedication; }
    public void setCourseMedication(CourseMedicationDTO courseMedication) { this.courseMedication = courseMedication; }
}