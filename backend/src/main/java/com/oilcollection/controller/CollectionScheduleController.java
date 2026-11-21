package com.oilcollection.controller;

import com.oilcollection.dto.CollectionScheduleDTO;
import com.oilcollection.model.CollectionSchedule;
import com.oilcollection.model.User;
import com.oilcollection.service.CollectionScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Collection Schedules", description = "Collection schedule management endpoints")
public class CollectionScheduleController {

    private final CollectionScheduleService scheduleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get all schedules with pagination")
    public ResponseEntity<Page<CollectionScheduleDTO>> getAllSchedules(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(scheduleService.getAllSchedules(pageable));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get all schedules as list")
    public ResponseEntity<List<CollectionScheduleDTO>> getAllSchedulesList() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<CollectionScheduleDTO> getScheduleById(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    @GetMapping("/collector/{collectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get schedules by collector")
    public ResponseEntity<Page<CollectionScheduleDTO>> getSchedulesByCollector(
            @PathVariable UUID collectorId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(scheduleService.getSchedulesByCollector(collectorId, pageable));
    }

    @GetMapping("/collector/{collectorId}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get schedules by collector and status")
    public ResponseEntity<List<CollectionScheduleDTO>> getSchedulesByCollectorAndStatus(
            @PathVariable UUID collectorId,
            @PathVariable CollectionSchedule.ScheduleStatus status) {
        return ResponseEntity.ok(scheduleService.getSchedulesByCollectorAndStatus(collectorId, status));
    }

    @GetMapping("/point/{pointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get schedules by collection point")
    public ResponseEntity<List<CollectionScheduleDTO>> getSchedulesByPoint(@PathVariable UUID pointId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByPoint(pointId));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get schedules by date")
    public ResponseEntity<List<CollectionScheduleDTO>> getSchedulesByDate(@PathVariable LocalDate date) {
        return ResponseEntity.ok(scheduleService.getSchedulesByDate(date));
    }

    @GetMapping("/collector/{collectorId}/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get upcoming schedules for collector")
    public ResponseEntity<List<CollectionScheduleDTO>> getUpcomingSchedules(@PathVariable UUID collectorId) {
        return ResponseEntity.ok(scheduleService.getUpcomingSchedules(collectorId));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get overdue schedules")
    public ResponseEntity<List<CollectionScheduleDTO>> getOverdueSchedules() {
        return ResponseEntity.ok(scheduleService.getOverdueSchedules());
    }

    @GetMapping("/collector/{collectorId}/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get schedules by collector and date range")
    public ResponseEntity<List<CollectionScheduleDTO>> getSchedulesByCollectorAndDateRange(
            @PathVariable UUID collectorId,
            @RequestParam LocalDate start,
            @RequestParam LocalDate end) {
        return ResponseEntity.ok(scheduleService.getSchedulesByCollectorAndDateRange(collectorId, start, end));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR', 'ESTABLISHMENT')")
    @Operation(summary = "Create new schedule")
    public ResponseEntity<CollectionScheduleDTO> createSchedule(
            @Valid @RequestBody CollectionScheduleDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(scheduleService.createSchedule(request, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update schedule")
    public ResponseEntity<CollectionScheduleDTO> updateSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody CollectionScheduleDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(scheduleService.updateSchedule(id, request, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign schedule to collector")
    public ResponseEntity<CollectionScheduleDTO> assignSchedule(
            @PathVariable UUID id,
            @RequestParam UUID collectorId,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(scheduleService.assignSchedule(id, collectorId, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Confirm/accept schedule (collector accepts pending schedule)")
    public ResponseEntity<CollectionScheduleDTO> confirmSchedule(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(scheduleService.confirmSchedule(id, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Start schedule (collector en route)")
    public ResponseEntity<CollectionScheduleDTO> startSchedule(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(scheduleService.startSchedule(id, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}/arrive")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Mark collector arrived at collection point")
    public ResponseEntity<CollectionScheduleDTO> arriveAtPoint(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(scheduleService.arriveAtPoint(id, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Complete schedule")
    public ResponseEntity<CollectionScheduleDTO> completeSchedule(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(scheduleService.completeSchedule(id, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Cancel schedule (must be at least 2 hours before scheduled time)")
    public ResponseEntity<CollectionScheduleDTO> cancelSchedule(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(scheduleService.cancelSchedule(id, reason, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reschedule (must be at least 2 hours before scheduled time)")
    public ResponseEntity<CollectionScheduleDTO> rescheduleSchedule(
            @PathVariable UUID id,
            @RequestParam LocalDate newDate,
            @RequestParam(required = false) LocalTime newStartTime,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(scheduleService.rescheduleSchedule(id, newDate, newStartTime, currentUser, ipAddress, userAgent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete schedule")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        scheduleService.deleteSchedule(id, currentUser, ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        String xrHeader = request.getHeader("X-Real-IP");
        if (xrHeader != null && !xrHeader.isEmpty()) {
            return xrHeader;
        }
        return request.getRemoteAddr();
    }
}