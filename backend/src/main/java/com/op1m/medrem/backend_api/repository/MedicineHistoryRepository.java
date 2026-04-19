package com.op1m.medrem.backend_api.repository;

import com.op1m.medrem.backend_api.entity.MedicineHistory;
import com.op1m.medrem.backend_api.entity.Reminder;
import com.op1m.medrem.backend_api.entity.User;
import com.op1m.medrem.backend_api.entity.enums.MedicineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface MedicineHistoryRepository extends JpaRepository<MedicineHistory, Long> {

    List<MedicineHistory> findByReminderUserOrderByScheduledTimeDesc(User user);

    @Query("select h from MedicineHistory h " +
            "left join fetch h.reminder r " +
            "left join fetch r.medicine m " +
            "left join fetch r.user u " +
            "where h.id = :id")
    MedicineHistory findWithReminderAndRelationsById(@Param("id") Long id);

    List<MedicineHistory> findByReminderUserAndStatusOrderByScheduledTimeDesc(User user, MedicineStatus status);

    @Query("SELECT mh FROM MedicineHistory mh " +
            "WHERE mh.reminder.user = :user AND mh.scheduledTime BETWEEN :start AND :end " +
            "ORDER BY mh.scheduledTime DESC")
    List<MedicineHistory> findByUserAndPeriod(@Param("user") User user,
                                              @Param("start") OffsetDateTime start,
                                              @Param("end") OffsetDateTime end);

    List<MedicineHistory> findByStatusAndScheduledTimeBetween(MedicineStatus status,
                                                              OffsetDateTime start,
                                                              OffsetDateTime end);

    List<MedicineHistory> findByStatusAndScheduledTimeBefore(MedicineStatus status,
                                                             OffsetDateTime scheduledTime);

    List<MedicineHistory> findByReminderAndScheduledTimeAfter(Reminder reminder,
                                                              OffsetDateTime time);

    @Query("select mh from MedicineHistory mh " +
            "join fetch mh.reminder r " +
            "join fetch r.medicine m " +
            "left join fetch r.user u " +
            "where r.user.id = :userId and mh.scheduledTime between :start and :end")
    List<MedicineHistory> findByUserIdAndPeriodWithFetch(@Param("userId") Long userId,
                                                         @Param("start") OffsetDateTime start,
                                                         @Param("end") OffsetDateTime end);


    @Modifying
    @Transactional
    @Query("delete from MedicineHistory mh where mh.reminder.id = :reminderId")
    void deleteByReminderId(@Param("reminderId") Long reminderId);


}