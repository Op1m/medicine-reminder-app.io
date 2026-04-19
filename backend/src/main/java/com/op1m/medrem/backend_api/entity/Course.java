package com.op1m.medrem.backend_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CourseMedication> medications = new ArrayList<>();

    public Course() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Course(User user, String name, LocalDate startDate, LocalDate endDate) {
        this();
        this.user = user;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void addMedication(CourseMedication medication) {
        this.medications.add(medication);
        medication.setCourse(this);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void removeMedication(CourseMedication medication) {
        this.medications.remove(medication);
        medication.setCourse(null);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
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

    public List<CourseMedication> getMedications() {
        return medications;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
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

    public void setMedications(List<CourseMedication> medications) {
        this.medications = medications;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}