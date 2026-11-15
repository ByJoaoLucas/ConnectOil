package com.oilcollection.controller;

import com.oilcollection.dto.CollectorCoverageDTO;
import com.oilcollection.model.User;
import com.oilcollection.service.CollectorCoverageService;
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
@RequestMapping("/api/collector-coverage")
@RequiredArgsConstructor
@Tag(name = "Collector Coverage", description = "Collector coverage area management")
public class CollectorCoverageController {

    private final CollectorCoverageService coverageService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all collector coverage areas with pagination")
    public ResponseEntity<Page<CollectorCoverageDTO>> getAllCoverage(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(coverageService.getAllCoverage(pageable));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all collector coverage areas as list")
    public ResponseEntity<List<CollectorCoverageDTO>> getAllCoverageList() {
        return ResponseEntity.ok(coverageService.getAllCoverage());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get coverage by ID")
    public ResponseEntity<CollectorCoverageDTO> getCoverageById(@PathVariable UUID id) {
        return ResponseEntity.ok(coverageService.getCoverageById(id));
    }

    @GetMapping("/collector/{collectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get coverage by collector")
    public ResponseEntity<List<CollectorCoverageDTO>> getCoverageByCollector(@PathVariable UUID collectorId) {
        return ResponseEntity.ok(coverageService.getCoverageByCollector(collectorId));
    }

    @GetMapping("/city/{city}/state/{state}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get coverage by city and state")
    public ResponseEntity<List<CollectorCoverageDTO>> getCoverageByCityAndState(
            @PathVariable String city,
            @PathVariable String state) {
        return ResponseEntity.ok(coverageService.getCoverageByCityAndState(city, state));
    }

    @GetMapping("/collector/{collectorId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR')")
    @Operation(summary = "Get active coverage for collector")
    public ResponseEntity<List<CollectorCoverageDTO>> getActiveCoverageByCollector(@PathVariable UUID collectorId) {
        return ResponseEntity.ok(coverageService.getActiveCoverageByCollector(collectorId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create new collector coverage")
    public ResponseEntity<CollectorCoverageDTO> createCoverage(
            @Valid @RequestBody CollectorCoverageDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(coverageService.createCoverage(request, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update collector coverage")
    public ResponseEntity<CollectorCoverageDTO> updateCoverage(
            @PathVariable UUID id,
            @Valid @RequestBody CollectorCoverageDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(coverageService.updateCoverage(id, request, currentUser, ipAddress, userAgent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete collector coverage")
    public ResponseEntity<Void> deleteCoverage(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        coverageService.deleteCoverage(id, currentUser, ipAddress, userAgent);
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