package com.op1m.medrem.backend_api.dto;

import com.op1m.medrem.backend_api.entity.*;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DTOMapper {

    public static UserDTO toUserDTO(User user) {
        if (user == null) return null;

        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhotoUrl(),
                user.getTelegramChatId(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getActive()
        );
    }

    public static MedicineDTO toMedicineDTO(Medicine medicine) {
        if (medicine == null) return null;

        return new MedicineDTO(
                medicine.getId(),
                medicine.getName(),
                medicine.getDosage(),
                medicine.getDescription(),
                medicine.getInstructions(),
                medicine.isActive(),
                medicine.getCreatedAt(),
                medicine.getUpdatedAt()
        );
    }

    public static ReminderDTO toReminderDTO(Reminder reminder) {
        if (reminder == null) return null;

        ReminderDTO dto = new ReminderDTO();
        dto.setId(reminder.getId());
        dto.setUser(toUserDTO(reminder.getUser()));
        dto.setMedicine(toMedicineDTO(reminder.getMedicine()));
        dto.setReminderTime(reminder.getReminderTime());
        dto.setIsActive(reminder.getIsActive());
        dto.setDaysOfWeek(reminder.getDaysOfWeek());
        dto.setCreatedAt(reminder.getCreatedAt());
        dto.setUpdatedAt(reminder.getUpdatedAt());

        // 👇 ДОБАВЛЯЕМ КУРСОВЫЕ ПОЛЯ
        dto.setSpecificDate(reminder.getSpecificDate());

        if (reminder.getCourseMedication() != null) {
            dto.setCourseMedicationId(reminder.getCourseMedication().getId());
            dto.setCourseMedication(toCourseMedicationDTO(reminder.getCourseMedication()));
        }

        return dto;
    }

    public static CourseMedicationDTO toCourseMedicationDTO(CourseMedication courseMedication) {
        if (courseMedication == null) return null;

        CourseMedicationDTO dto = new CourseMedicationDTO();
        dto.setId(courseMedication.getId());
        dto.setCourseId(courseMedication.getCourse() != null ? courseMedication.getCourse().getId() : null);
        dto.setMedicineName(courseMedication.getMedicineName());
        dto.setDosage(courseMedication.getDosage());
        dto.setDescription(courseMedication.getDescription());
        dto.setInstructions(courseMedication.getInstructions());
        dto.setMealMode(courseMedication.getMealMode() != null ? courseMedication.getMealMode().name() : null);
        dto.setTimeOfDay(courseMedication.getTimeOfDay());
        dto.setScheduleType(courseMedication.getScheduleType() != null ? courseMedication.getScheduleType().name() : null);
        dto.setIntervalDays(courseMedication.getIntervalDays());
        dto.setGeneratedMedicineId(courseMedication.getGeneratedMedicineId());
        dto.setIsActive(courseMedication.getActive());

        return dto;
    }

    public static MedicineHistoryDTO toMedicineHistoryDTO(MedicineHistory history) {
        if (history == null) return null;

        ReminderDTO reminderDTO = null;
        try {
            if (history.getReminder() != null) {
                reminderDTO = toReminderDTO(history.getReminder());
            }
        } catch (Exception e) {
            reminderDTO = null;
        }

        return new MedicineHistoryDTO(
                history.getId(),
                reminderDTO,
                history.getScheduledTime(),
                history.getTakenAt(),
                history.getStatus(),
                history.getNotes(),
                history.getCreatedAt()
        );
    }

    public static CategoryDTO toCategoryDTO(Category category) {
        if (category == null) return null;

        Set<MedicineDTO> medicineDTOs = new HashSet<>();
        try {
            if (category.getMedicines() != null) {
                medicineDTOs = category.getMedicines().stream()
                        .map(DTOMapper::toMedicineDTO)
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            medicineDTOs = new HashSet<>();
        }

        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIsActive(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                medicineDTOs
        );
    }

    public static CategoryDTO toCategoryDTOSimple(Category category) {
        if (category == null) return null;

        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIsActive(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                null
        );
    }
}