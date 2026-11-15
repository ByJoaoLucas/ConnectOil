package com.oilcollection.controller;

import com.oilcollection.dto.CollectionRecordDTO;
import com.oilcollection.dto.CollectorPerformanceDTO;
import com.oilcollection.dto.DailyVolumeDTO;
import com.oilcollection.dto.OilTypeVolumeDTO;
import com.oilcollection.model.User;
import com.oilcollection.service.CollectionRecordService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Tag(name = "Collection Records", description = "Collection record management endpoints")
public class CollectionRecordController {

    private final CollectionRecordService recordService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get all records with pagination")
    public ResponseEntity<Page<CollectionRecordDTO>> getAllRecords(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(recordService.getAllRecords(pageable));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get all records as list")
    public ResponseEntity<List<CollectionRecordDTO>> getAllRecordsList() {
        return ResponseEntity.ok(recordService.getAllRecords());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get record by ID")
    public ResponseEntity<CollectionRecordDTO> getRecordById(@PathVariable UUID id) {
        return ResponseEntity.ok(recordService.getRecordById(id));
    }

    @GetMapping("/point/{pointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get records by collection point")
    public ResponseEntity<Page<CollectionRecordDTO>> getRecordsByPoint(
            @PathVariable UUID pointId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(recordService.getRecordsByPoint(pointId, pageable));
    }

    @GetMapping("/collector/{collectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get records by collector")
    public ResponseEntity<Page<CollectionRecordDTO>> getRecordsByCollector(
            @PathVariable UUID collectorId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(recordService.getRecordsByCollector(collectorId, pageable));
    }

    @GetMapping("/collector/{collectorId}/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get records by collector and date range")
    public ResponseEntity<List<CollectionRecordDTO>> getRecordsByCollectorAndDateRange(
            @PathVariable UUID collectorId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(recordService.getRecordsByCollectorAndDateRange(collectorId, start, end));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get records by date range")
    public ResponseEntity<List<CollectionRecordDTO>> getRecordsByDateRange(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(recordService.getRecordsByDateRange(start, end));
    }

    @GetMapping("/pending-verification")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get pending verification records")
    public ResponseEntity<List<CollectionRecordDTO>> getPendingVerificationRecords() {
        return ResponseEntity.ok(recordService.getPendingVerificationRecords());
    }

    @GetMapping("/collector/{collectorId}/pending-verification")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get pending verification records by collector")
    public ResponseEntity<List<CollectionRecordDTO>> getPendingVerificationByCollector(
            @PathVariable UUID collectorId) {
        return ResponseEntity.ok(recordService.getPendingVerificationByCollector(collectorId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Create new collection record")
    public ResponseEntity<CollectionRecordDTO> createRecord(
            @Valid @RequestBody CollectionRecordDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(recordService.createRecord(request, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Update collection record")
    public ResponseEntity<CollectionRecordDTO> updateRecord(
            @PathVariable UUID id,
            @Valid @RequestBody CollectionRecordDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(recordService.updateRecord(id, request, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Verify collection record (generates certificate)")
    public ResponseEntity<CollectionRecordDTO> verifyRecord(
            @PathVariable UUID id,
            @RequestParam UUID managerId,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(recordService.verifyRecord(id, managerId, currentUser, ipAddress, userAgent));
    }

    // US05 - Establishment confirms collection with confirmation code
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ESTABLISHMENT')")
    @Operation(summary = "Confirm collection by establishment using confirmation code")
    public ResponseEntity<CollectionRecordDTO> confirmCollection(
            @PathVariable UUID id,
            @RequestParam String confirmationCode,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(recordService.confirmCollectionByEstablishment(id, confirmationCode, currentUser, ipAddress, userAgent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete collection record")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        recordService.deleteRecord(id, currentUser, ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }

    // US07 - Generate/download certificate
    @GetMapping("/{id}/certificate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR', 'ESTABLISHMENT')")
    @Operation(summary = "Get certificate data for PDF generation")
    public ResponseEntity<CollectionRecordDTO> getCertificate(@PathVariable UUID id) {
        return ResponseEntity.ok(recordService.generateCertificate(id));
    }

    // Reports
    @GetMapping("/reports/volume/collector/{collectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get total volume by collector")
    public ResponseEntity<BigDecimal> getTotalVolumeByCollector(
            @PathVariable UUID collectorId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(recordService.getTotalVolumeByCollector(collectorId, start, end));
    }

    @GetMapping("/reports/volume/point/{pointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get total volume by collection point")
    public ResponseEntity<BigDecimal> getTotalVolumeByPoint(
            @PathVariable UUID pointId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(recordService.getTotalVolumeByPoint(pointId, start, end));
    }

    @GetMapping("/reports/count/collector/{collectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get collection count by collector")
    public ResponseEntity<Long> getCollectionCountByCollector(
            @PathVariable UUID collectorId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(recordService.getCollectionCountByCollector(collectorId, start, end));
    }

    @GetMapping("/reports/quality/collector/{collectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get average quality by collector")
    public ResponseEntity<Double> getAverageQualityByCollector(
            @PathVariable UUID collectorId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(recordService.getAverageQualityByCollector(collectorId, start, end));
    }

    @GetMapping("/reports/daily-volume")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get daily volume report")
    public ResponseEntity<List<DailyVolumeDTO>> getDailyVolumeReport(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(recordService.getDailyVolumeReport(start, end));
    }

    @GetMapping("/reports/collector-performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get collector performance report")
    public ResponseEntity<List<CollectorPerformanceDTO>> getCollectorPerformanceReport(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(recordService.getCollectorPerformanceReport(start, end));
    }

    @GetMapping("/reports/oil-type-volume")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get oil type volume report")
    public ResponseEntity<List<OilTypeVolumeDTO>> getOilTypeVolumeReport(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(recordService.getOilTypeVolumeReport(start, end));
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