package com.op1m.medrem.backend_api.controller;

import com.op1m.medrem.backend_api.dto.MedicineHistoryDTO;
import com.op1m.medrem.backend_api.dto.DTOMapper;
import com.op1m.medrem.backend_api.entity.MedicineHistory;
import com.op1m.medrem.backend_api.entity.User;
import com.op1m.medrem.backend_api.entity.enums.MedicineStatus;
import com.op1m.medrem.backend_api.service.MedicineHistoryService;
import com.op1m.medrem.backend_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medicine-history")
public class MedicineHistoryController {
    @Autowired
    private MedicineHistoryService medicineHistoryService;

    @Autowired
    private UserService userService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MedicineHistoryDTO>> getUserHistory(@PathVariable Long userId) {
        try {
            List<MedicineHistory> history = medicineHistoryService.getUserMedicineHistory(userId);
            List<MedicineHistoryDTO> historyDTO = history.stream()
                    .map(DTOMapper::toMedicineHistoryDTO)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(historyDTO, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<MedicineHistoryDTO>> getUserHistoryByStatus(@PathVariable Long userId, @PathVariable MedicineStatus status) {
        try {
            List<MedicineHistory> history = medicineHistoryService.getMedicineHistoryByStatus(userId, status);
            List<MedicineHistoryDTO> historyDTO = history.stream()
                    .map(DTOMapper::toMedicineHistoryDTO)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(historyDTO, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/user/{userId}/period")
    public ResponseEntity<List<MedicineHistoryDTO>> getHistoryByPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        try {
            List<MedicineHistory> history = medicineHistoryService.getHistoryByPeriod(userId, start, end);
            List<MedicineHistoryDTO> historyDTO = history.stream()
                    .map(DTOMapper::toMedicineHistoryDTO)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(historyDTO, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/{historyId}/mark-taken")
    public ResponseEntity<MedicineHistoryDTO> markAsTaken(@PathVariable Long historyId, @RequestBody(required = false) MarkTakenRequest request) {
        try {
            String notes = request != null ? request.getNotes() : null;
            MedicineHistory history = medicineHistoryService.markAsTaken(historyId, notes);
            MedicineHistoryDTO historyDTO = DTOMapper.toMedicineHistoryDTO(history);
            return new ResponseEntity<>(historyDTO, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @PostMapping("/{historyId}/postpone")
    public ResponseEntity<MedicineHistoryDTO> postponeHistory(
            @PathVariable Long historyId,
            @RequestParam(defaultValue = "10") int minutes) {
        try {
            MedicineHistory history = medicineHistoryService.findById(historyId);
            if (history == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            OffsetDateTime newScheduledTime = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(minutes);

            MedicineHistory postponedHistory = new MedicineHistory(history.getReminder(), newScheduledTime);
            postponedHistory.setStatus(MedicineStatus.POSTPONED);
            postponedHistory.setNotes("Отложено на " + minutes + " минут");

            MedicineHistory saved = medicineHistoryService.save(postponedHistory);

            MedicineHistoryDTO dto = DTOMapper.toMedicineHistoryDTO(saved);
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/reminder/{reminderId}/postpone")
    public ResponseEntity<?> postponeReminder(
            @PathVariable Long reminderId,
            @RequestParam(defaultValue = "10") int minutes,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);

            MedicineHistory postponed = medicineHistoryService.postponeReminder(
                    reminderId, user.getTelegramChatId(), minutes);

            // ВРЕМЕННЫЙ ОБХОД: возвращаем только нужные поля в Map
            Map<String, Object> response = new HashMap<>();
            response.put("id", postponed.getId());
            response.put("status", postponed.getStatus().toString());
            response.put("scheduledTime", postponed.getScheduledTime().toString());
            response.put("notes", postponed.getNotes());

            // Добавляем минимальную информацию о reminder
            if (postponed.getReminder() != null) {
                Map<String, Object> reminderMap = new HashMap<>();
                reminderMap.put("id", postponed.getReminder().getId());
                response.put("reminder", reminderMap);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{historyId}/mark-skipped")
    public ResponseEntity<MedicineHistoryDTO> markAsSkipped(@PathVariable Long historyId) {
        try {
            MedicineHistory history = medicineHistoryService.markAsSkipped(historyId);
            MedicineHistoryDTO historyDTO = DTOMapper.toMedicineHistoryDTO(history);
            return new ResponseEntity<>(historyDTO, HttpStatus.OK);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{historyId}/mark-skipped")
    public ResponseEntity<MedicineHistoryDTO> markAsSkippedPost(@PathVariable Long historyId) {
        return markAsSkipped(historyId);
    }

    @PostMapping("/schedule")
    public ResponseEntity<?> createScheduleDose(@RequestBody ScheduleDoseRequest request) {
        try {
            OffsetDateTime scheduled = null;
            if (request.getScheduledTime() != null) {
                try {
                    scheduled = OffsetDateTime.parse(request.getScheduledTime());
                } catch (DateTimeParseException ex) {
                    scheduled = OffsetDateTime.parse(request.getScheduledTime() + "Z");
                }
            }
            MedicineHistory history = medicineHistoryService.createScheduleDose(request.getReminderId(), scheduled);
            MedicineHistoryDTO dto = DTOMapper.toMedicineHistoryDTO(history);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            err.put("trace", e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @PostMapping("/check_missed")
    public ResponseEntity<Void> checkMissedDoses() {
        medicineHistoryService.checkAndMarkMissedDoses();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public static class MarkTakenRequest {
        private String notes;
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class ScheduleDoseRequest {
        private Long reminderId;
        private String scheduledTime;
        public Long getReminderId() { return reminderId; }
        public void setReminderId(Long reminderId) { this.reminderId = reminderId; }
        public String getScheduledTime() { return scheduledTime; }
        public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }
    }
}