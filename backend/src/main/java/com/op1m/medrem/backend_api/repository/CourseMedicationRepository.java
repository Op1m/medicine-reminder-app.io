package com.op1m.medrem.backend_api.repository;

import com.op1m.medrem.backend_api.entity.CourseMedication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseMedicationRepository extends JpaRepository<CourseMedication, Long> {
    List<CourseMedication> findByCourseIdOrderByIdAsc(Long courseId);
    List<CourseMedication> findByCourseIdAndIsActiveTrueOrderByIdAsc(Long courseId);
}