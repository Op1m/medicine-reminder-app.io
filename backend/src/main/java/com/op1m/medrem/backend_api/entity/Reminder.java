package com.op1m.medrem.backend_api.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "reminders")
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine", nullable = false)
    private Medicine medicine;

    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "days_of_week")
    private String daysOfWeek = "everyday";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "course_medication_id")
private CourseMedication courseMedication;

    public Reminder() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Reminder(User user, Medicine medicine, LocalTime reminderTime) {
        this();
        this.user = user;
        this.medicine = medicine;
        this.reminderTime = reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setDaysOfWeek(String daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Medicine getMedicine() { return medicine; }
    public LocalTime getReminderTime() { return reminderTime; }
    public Boolean getIsActive() { return isActive; }
    public String getDaysOfWeek() { return daysOfWeek; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}