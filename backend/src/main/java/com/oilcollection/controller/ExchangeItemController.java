package com.oilcollection.controller;

import com.oilcollection.dto.ExchangeItemDTO;
import com.oilcollection.model.User;
import com.oilcollection.service.ExchangeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exchange-items")
@RequiredArgsConstructor
@Tag(name = "Exchange Items", description = "Exchange item management for oil exchange schedules")
public class ExchangeItemController {

    private final ExchangeItemService exchangeItemService;

    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COLLECTOR', 'ESTABLISHMENT')")
    @Operation(summary = "Get exchange items by schedule")
    public ResponseEntity<List<ExchangeItemDTO>> getBySchedule(@PathVariable UUID scheduleId) {
        return ResponseEntity.ok(exchangeItemService.getByScheduleId(scheduleId));
    }

    @PostMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create exchange items for schedule")
    public ResponseEntity<List<ExchangeItemDTO>> createForSchedule(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody List<ExchangeItemDTO.ExchangeItemRequestDTO> requests,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(exchangeItemService.createForSchedule(scheduleId, requests, currentUser, ipAddress, userAgent));
    }

    @DeleteMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete all exchange items for schedule")
    public ResponseEntity<Void> deleteBySchedule(
            @PathVariable UUID scheduleId,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        exchangeItemService.deleteByScheduleId(scheduleId, currentUser, ipAddress, userAgent);
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