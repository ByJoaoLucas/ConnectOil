package com.oilcollection.controller;

import com.oilcollection.dto.SystemConfigDTO;
import com.oilcollection.model.User;
import com.oilcollection.service.SystemConfigService;
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
@RequestMapping("/api/system-config")
@RequiredArgsConstructor
@Tag(name = "System Configuration", description = "System configuration management")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all system configurations with pagination")
    public ResponseEntity<Page<SystemConfigDTO>> getAllConfigs(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(systemConfigService.getAllConfigs(pageable));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all system configurations as list")
    public ResponseEntity<List<SystemConfigDTO>> getAllConfigsList() {
        return ResponseEntity.ok(systemConfigService.getAllConfigs());
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get configuration by key")
    public ResponseEntity<SystemConfigDTO> getConfigByKey(@PathVariable String key) {
        return ResponseEntity.ok(systemConfigService.getConfigByKey(key));
    }

    @GetMapping("/value/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get configuration value by key")
    public ResponseEntity<String> getConfigValue(@PathVariable String key) {
        return ResponseEntity.ok(systemConfigService.getConfigValue(key));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create or update configuration")
    public ResponseEntity<SystemConfigDTO> setConfig(
            @Valid @RequestBody SystemConfigDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(systemConfigService.setConfig(request, currentUser, ipAddress, userAgent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete configuration")
    public ResponseEntity<Void> deleteConfig(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        systemConfigService.deleteConfig(id, currentUser, ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }

    // Convenience endpoints for common configs
    @GetMapping("/price/base-oil")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Get base oil price per liter")
    public ResponseEntity<java.math.BigDecimal> getBaseOilPrice() {
        return ResponseEntity.ok(systemConfigService.getBaseOilPricePerLiter());
    }

    @GetMapping("/threshold/discrepancy")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get discrepancy threshold percentage")
    public ResponseEntity<Integer> getDiscrepancyThreshold() {
        return ResponseEntity.ok(systemConfigService.getDiscrepancyThresholdPercentage());
    }

    @GetMapping("/cancellation/hours-before")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get cancellation hours before schedule")
    public ResponseEntity<Integer> getCancellationHours() {
        return ResponseEntity.ok(systemConfigService.getCancellationHoursBefore());
    }

    @GetMapping("/security/lockout/max-attempts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get account lockout max attempts")
    public ResponseEntity<Integer> getLockoutMaxAttempts() {
        return ResponseEntity.ok(systemConfigService.getAccountLockoutMaxAttempts());
    }

    @GetMapping("/security/lockout/duration")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get account lockout duration in minutes")
    public ResponseEntity<Integer> getLockoutDuration() {
        return ResponseEntity.ok(systemConfigService.getAccountLockoutDurationMinutes());
    }

    @GetMapping("/security/password/min-length")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get password minimum length")
    public ResponseEntity<Integer> getPasswordMinLength() {
        return ResponseEntity.ok(systemConfigService.getPasswordMinLength());
    }

    @GetMapping("/security/password/requirements")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get password requirements")
    public ResponseEntity<java.util.Map<String, Boolean>> getPasswordRequirements() {
        java.util.Map<String, Boolean> requirements = new java.util.HashMap<>();
        requirements.put("requireUppercase", systemConfigService.getPasswordRequireUppercase());
        requirements.put("requireLowercase", systemConfigService.getPasswordRequireLowercase());
        requirements.put("requireNumber", systemConfigService.getPasswordRequireNumber());
        requirements.put("requireSpecial", systemConfigService.getPasswordRequireSpecial());
        return ResponseEntity.ok(requirements);
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