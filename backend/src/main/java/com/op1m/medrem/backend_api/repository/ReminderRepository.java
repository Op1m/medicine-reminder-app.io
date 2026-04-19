package com.op1m.medrem.backend_api.repository;

import com.op1m.medrem.backend_api.entity.User;
import com.op1m.medrem.backend_api.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    @Query("select r from Reminder r " +
            "left join fetch r.user " +
            "left join fetch r.medicine " +
            "where r.user = :user")
    List<Reminder> findByUser(@Param("user") User user);

    @Query("select r from Reminder r " +
            "left join fetch r.user " +
            "left join fetch r.medicine " +
            "where r.user = :user and r.isActive = true")
    List<Reminder> findByUserAndIsActiveTrue(@Param("user") User user);

    List<Reminder> findByIsActiveTrue();

    @Query("select r from Reminder r " +
            "left join fetch r.medicine m " +
            "left join fetch r.user u " +
            "where r.isActive = true")
    List<Reminder> findAllActiveWithUserAndMedicine();

    @Query("select r from Reminder r " +
            "left join fetch r.medicine m " +
            "left join fetch r.user u " +
            "where r.user.id = :userId")
    List<Reminder> findByUserIdWithMedicine(@Param("userId") Long userId);

    @Query("SELECT r FROM Reminder r " +
           "JOIN FETCH r.user u " +
           "JOIN FETCH r.medicine m " +
           "WHERE r.isActive = true AND r.specificDate = :date")
    List<Reminder> findActiveBySpecificDate(@Param("date") LocalDate date);

    @Query("SELECT r FROM Reminder r WHERE r.courseMedication.id = :courseMedicationId")
    List<Reminder> findByCourseMedicationId(@Param("courseMedicationId") Long courseMedicationId);

        List<Reminder> findByCourseMedication_Course_Id(Long courseId);
}