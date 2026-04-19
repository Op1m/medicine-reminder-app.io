package com.op1m.medrem.backend_api.repository;

import com.op1m.medrem.backend_api.entity.Course;
import com.op1m.medrem.backend_api.entity.Reminder;
import com.op1m.medrem.backend_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUserOrderByCreatedAtDesc(User user);
    List<Course> findByUserAndIsActiveTrueOrderByCreatedAtDesc(User user);
}