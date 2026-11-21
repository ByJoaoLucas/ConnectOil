package com.oilcollection.controller;

import com.oilcollection.dto.CollectionPointDTO;
import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.User;
import com.oilcollection.service.CollectionPointService;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/collection-points")
@RequiredArgsConstructor
@Tag(name = "Collection Points", description = "Collection point management endpoints")
public class CollectionPointController {

    private final CollectionPointService pointService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR', 'CLIENT', 'ESTABLISHMENT')")
    @Operation(summary = "Get all collection points with pagination")
    public ResponseEntity<Page<CollectionPointDTO>> getAllPoints(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(pointService.getAllPoints(pageable));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR', 'CLIENT', 'ESTABLISHMENT')")
    @Operation(summary = "Get all collection points as list")
    public ResponseEntity<List<CollectionPointDTO>> getAllPointsList() {
        return ResponseEntity.ok(pointService.getAllPoints());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR', 'CLIENT', 'ESTABLISHMENT')")
    @Operation(summary = "Get collection point by ID")
    public ResponseEntity<CollectionPointDTO> getPointById(@PathVariable UUID id) {
        return ResponseEntity.ok(pointService.getPointById(id));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get collection points by status")
    public ResponseEntity<Page<CollectionPointDTO>> getPointsByStatus(
            @PathVariable CollectionPoint.PointStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(pointService.getPointsByStatus(status, pageable));
    }

    @GetMapping("/collector/{collectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get collection points by collector")
    public ResponseEntity<Page<CollectionPointDTO>> getPointsByCollector(
            @PathVariable UUID collectorId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(pointService.getPointsByCollector(collectorId, pageable));
    }

    @GetMapping("/collector/{collectorId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get active collection points by collector")
    public ResponseEntity<List<CollectionPointDTO>> getActivePointsByCollector(@PathVariable UUID collectorId) {
        return ResponseEntity.ok(pointService.getActivePointsByCollector(collectorId));
    }

    @GetMapping("/near-full")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get near-full collection points")
    public ResponseEntity<List<CollectionPointDTO>> getNearFullPoints() {
        return ResponseEntity.ok(pointService.getNearFullPoints());
    }

    @GetMapping("/location")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR', 'CLIENT', 'ESTABLISHMENT')")
    @Operation(summary = "Get collection points by bounding box")
    public ResponseEntity<List<CollectionPointDTO>> getPointsByLocation(
            @RequestParam BigDecimal minLat,
            @RequestParam BigDecimal maxLat,
            @RequestParam BigDecimal minLon,
            @RequestParam BigDecimal maxLon) {
        return ResponseEntity.ok(pointService.getPointsByLocation(minLat, maxLat, minLon, maxLon));
    }

    @GetMapping("/city/{city}/state/{state}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR', 'CLIENT', 'ESTABLISHMENT')")
    @Operation(summary = "Get collection points by city and state")
    public ResponseEntity<List<CollectionPointDTO>> getPointsByCityAndState(
            @PathVariable String city,
            @PathVariable String state) {
        return ResponseEntity.ok(pointService.getPointsByCityAndState(city, state));
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get unassigned collection points")
    public ResponseEntity<List<CollectionPointDTO>> getUnassignedPoints() {
        return ResponseEntity.ok(pointService.getUnassignedPoints());
    }

    // US02 - Establishment endpoints
    @GetMapping("/my-points")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    @Operation(summary = "Get collection points owned by the establishment")
    public ResponseEntity<Page<CollectionPointDTO>> getMyPoints(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(pointService.getPointsByEstablishment(currentUser.getId(), pageable));
    }

    @GetMapping("/my-points/list")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    @Operation(summary = "Get collection points owned by the establishment as list")
    public ResponseEntity<List<CollectionPointDTO>> getMyPointsList(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(pointService.getPointsByEstablishment(currentUser.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTABLISHMENT')")
    @Operation(summary = "Create new collection point (ADMIN can specify owner, ESTABLISHMENT becomes owner automatically)")
    public ResponseEntity<CollectionPointDTO> createPoint(
            @Valid @RequestBody CollectionPointDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(pointService.createPoint(request, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTABLISHMENT')")
    @Operation(summary = "Update collection point (ESTABLISHMENT can only update own points)")
    public ResponseEntity<CollectionPointDTO> updatePoint(
            @PathVariable UUID id,
            @Valid @RequestBody CollectionPointDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(pointService.updatePoint(id, request, currentUser, ipAddress, userAgent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete collection point (only ADMIN)")
    public ResponseEntity<Void> deletePoint(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        pointService.deletePoint(id, currentUser, ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{pointId}/collector/{collectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTABLISHMENT')")
    @Operation(summary = "Assign collector to collection point (ESTABLISHMENT can only assign to own points)")
    public ResponseEntity<CollectionPointDTO> assignCollector(
            @PathVariable UUID pointId,
            @PathVariable UUID collectorId,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(pointService.assignCollector(pointId, collectorId, currentUser, ipAddress, userAgent));
    }

    @DeleteMapping("/{pointId}/collector")
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTABLISHMENT')")
    @Operation(summary = "Unassign collector from collection point (ESTABLISHMENT can only unassign from own points)")
    public ResponseEntity<CollectionPointDTO> unassignCollector(
            @PathVariable UUID pointId,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(pointService.unassignCollector(pointId, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{pointId}/volume")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR', 'ESTABLISHMENT')")
    @Operation(summary = "Update collection point volume (ESTABLISHMENT can only update own points)")
    public ResponseEntity<CollectionPointDTO> updateVolume(
            @PathVariable UUID pointId,
            @RequestParam Integer volumeLiters,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(pointService.updateVolume(pointId, volumeLiters, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{pointId}/volume/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR', 'ESTABLISHMENT')")
    @Operation(summary = "Add volume to collection point (ESTABLISHMENT can only add to own points)")
    public ResponseEntity<CollectionPointDTO> addVolume(
            @PathVariable UUID pointId,
            @RequestParam Integer additionalLiters,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(pointService.addVolume(pointId, additionalLiters, currentUser, ipAddress, userAgent));
    }

    @PutMapping("/{pointId}/volume/remove")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR', 'ESTABLISHMENT')")
    @Operation(summary = "Remove volume from collection point (ESTABLISHMENT can only remove from own points)")
    public ResponseEntity<CollectionPointDTO> removeVolume(
            @PathVariable UUID pointId,
            @RequestParam Integer removedLiters,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(pointService.removeVolume(pointId, removedLiters, currentUser, ipAddress, userAgent));
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