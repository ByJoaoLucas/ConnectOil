package com.oilcollection.controller;

import com.oilcollection.dto.IncidentDTO;
import com.oilcollection.model.User;
import com.oilcollection.service.IncidentService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Incident reporting and management")
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get all incidents with pagination")
    public ResponseEntity<Page<IncidentDTO>> getAllIncidents(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(incidentService.getAllIncidents(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get incident by ID")
    public ResponseEntity<IncidentDTO> getIncidentById(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }

    @GetMapping("/point/{pointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get incidents by collection point")
    public ResponseEntity<List<IncidentDTO>> getIncidentsByPoint(@PathVariable UUID pointId) {
        return ResponseEntity.ok(incidentService.getIncidentsByPoint(pointId));
    }

    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get incidents by schedule")
    public ResponseEntity<List<IncidentDTO>> getIncidentsBySchedule(@PathVariable UUID scheduleId) {
        return ResponseEntity.ok(incidentService.getIncidentsBySchedule(scheduleId));
    }

    @GetMapping("/reporter/{reporterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get incidents by reporter")
    public ResponseEntity<List<IncidentDTO>> getIncidentsByReporter(@PathVariable UUID reporterId) {
        return ResponseEntity.ok(incidentService.getIncidentsByReporter(reporterId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get incidents by status")
    public ResponseEntity<Page<IncidentDTO>> getIncidentsByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(incidentService.getIncidentsByStatus(status, pageable));
    }

    @GetMapping("/point/{pointId}/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get recent incidents by point")
    public ResponseEntity<List<IncidentDTO>> getRecentIncidentsByPoint(
            @PathVariable UUID pointId,
            @RequestParam java.time.LocalDateTime since) {
        return ResponseEntity.ok(incidentService.getRecentIncidentsByPoint(pointId, since));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR', 'ESTABLISHMENT')")
    @Operation(summary = "Create new incident report")
    public ResponseEntity<IncidentDTO> createIncident(
            @Valid @RequestBody IncidentDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(incidentService.createIncident(request, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Update incident")
    public ResponseEntity<IncidentDTO> updateIncident(
            @PathVariable UUID id,
            @Valid @RequestBody IncidentDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(incidentService.updateIncident(id, request, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Resolve incident")
    public ResponseEntity<IncidentDTO> resolveIncident(
            @PathVariable UUID id,
            @RequestParam String resolutionNotes,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(incidentService.resolveIncident(id, currentUser, resolutionNotes, ipAddress, userAgent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete incident")
    public ResponseEntity<Void> deleteIncident(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        incidentService.deleteIncident(id, currentUser, ipAddress, userAgent);
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