package com.oilcollection.service;

import com.oilcollection.dto.SystemConfigDTO;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.SystemConfig;
import com.oilcollection.model.User;
import com.oilcollection.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    private final SystemConfigRepository configRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<SystemConfigDTO> getAllConfigs(Pageable pageable) {
        return configRepository.findAll(pageable).map(SystemConfigDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<SystemConfigDTO> getAllConfigs() {
        return configRepository.findAll().stream().map(SystemConfigDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public SystemConfigDTO getConfigByKey(String key) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfigDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("System Config", "key", key));
    }

    public String getConfigValue(String key) {
        return configRepository.findConfigValueByConfigKey(key);
    }

    public Optional<SystemConfig> findByKey(String key) {
        return configRepository.findByConfigKey(key);
    }

    public BigDecimal getBaseOilPricePerLiter() {
        String value = getConfigValue("base_oil_price_per_liter");
        return value != null ? new BigDecimal(value) : BigDecimal.ZERO;
    }

    public int getDiscrepancyThresholdPercentage() {
        String value = getConfigValue("discrepancy_threshold_percentage");
        return value != null ? Integer.parseInt(value) : 20;
    }

    public int getCancellationHoursBefore() {
        String value = getConfigValue("cancellation_hours_before");
        return value != null ? Integer.parseInt(value) : 2;
    }

    public int getAccountLockoutMaxAttempts() {
        String value = getConfigValue("account_lockout_max_attempts");
        return value != null ? Integer.parseInt(value) : 5;
    }

    public int getAccountLockoutDurationMinutes() {
        String value = getConfigValue("account_lockout_duration_minutes");
        return value != null ? Integer.parseInt(value) : 30;
    }

    public int getPasswordMinLength() {
        String value = getConfigValue("password_min_length");
        return value != null ? Integer.parseInt(value) : 8;
    }

    public boolean getPasswordRequireUppercase() {
        String value = getConfigValue("password_require_uppercase");
        return value != null && Boolean.parseBoolean(value);
    }

    public boolean getPasswordRequireLowercase() {
        String value = getConfigValue("password_require_lowercase");
        return value != null && Boolean.parseBoolean(value);
    }

    public boolean getPasswordRequireNumber() {
        String value = getConfigValue("password_require_number");
        return value != null && Boolean.parseBoolean(value);
    }

    public boolean getPasswordRequireSpecial() {
        String value = getConfigValue("password_require_special");
        return value != null && Boolean.parseBoolean(value);
    }

    @Transactional
    public SystemConfigDTO setConfig(SystemConfigDTO request, User createdBy, String ipAddress, String userAgent) {
        Optional<SystemConfig> existing = configRepository.findByConfigKey(request.getConfigKey());
        SystemConfig config = existing.orElse(new SystemConfig());

        String oldValues = existing.map(this::toJson).orElse(null);

        config.setConfigKey(request.getConfigKey());
        config.setConfigValue(request.getConfigValue());
        config.setDescription(request.getDescription());
        config.setDataType(request.getDataType());

        SystemConfig savedConfig = configRepository.save(config);

        // Log audit
        if (existing.isPresent()) {
            auditLogService.logUpdate("SYSTEM_CONFIG", savedConfig.getId(), createdBy,
                    oldValues, toJson(savedConfig), ipAddress, userAgent);
        } else {
            auditLogService.logCreate("SYSTEM_CONFIG", savedConfig.getId(), createdBy,
                    toJson(savedConfig), ipAddress, userAgent);
        }

        log.info("System config saved: {} = {} by {}", config.getConfigKey(), config.getConfigValue(), createdBy.getEmail());
        return SystemConfigDTO.fromEntity(savedConfig);
    }

    @Transactional
    public void deleteConfig(UUID id, User deletedBy, String ipAddress, String userAgent) {
        SystemConfig config = configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System Config", "id", id));

        String oldValues = toJson(config);

        configRepository.delete(config);

        // Log audit
        auditLogService.logDelete("SYSTEM_CONFIG", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("System config deleted: {} by {}", config.getConfigKey(), deletedBy.getEmail());
    }

    private String toJson(SystemConfig config) {
        return String.format(
                "{\"id\":\"%s\",\"key\":\"%s\",\"value\":\"%s\",\"description\":\"%s\",\"dataType\":\"%s\"}",
                config.getId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getDescription(),
                config.getDataType()
        );
    }
}