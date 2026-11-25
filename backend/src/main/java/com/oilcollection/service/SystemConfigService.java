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

    private static final String LOG_PREFIX = "[SystemConfigService]";

    @Transactional(readOnly = true)
    public Page<SystemConfigDTO> getAllConfigs(Pageable pageable) {
        log.debug("{} Buscando todas as configurações do sistema com paginação: {}", LOG_PREFIX, pageable);
        return configRepository.findAll(pageable).map(SystemConfigDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<SystemConfigDTO> getAllConfigs() {
        log.debug("{} Buscando todas as configurações do sistema como lista", LOG_PREFIX);
        return configRepository.findAll().stream().map(SystemConfigDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public SystemConfigDTO getConfigByKey(String key) {
        log.debug("{} Buscando configuração por chave: {}", LOG_PREFIX, key);
        return configRepository.findByConfigKey(key)
                .map(SystemConfigDTO::fromEntity)
                .orElseThrow(() -> {
                    log.warn("{} Configuração não encontrada: {}", LOG_PREFIX, key);
                    return new ResourceNotFoundException("System Config", "key", key);
                });
    }

    public String getConfigValue(String key) {
        log.debug("{} Obtendo valor da configuração: {}", LOG_PREFIX, key);
        return configRepository.findConfigValueByConfigKey(key).orElse(null);
    }

    public Optional<SystemConfig> findByKey(String key) {
        log.debug("{} Buscando configuração opcional por chave: {}", LOG_PREFIX, key);
        return configRepository.findByConfigKey(key);
    }

    public BigDecimal getBaseOilPricePerLiter() {
        log.debug("{} Obtendo preço base do óleo por litro", LOG_PREFIX);
        String value = getConfigValue("base_oil_price_per_liter");
        return value != null ? new BigDecimal(value) : BigDecimal.ZERO;
    }

    public int getDiscrepancyThresholdPercentage() {
        log.debug("{} Obtendo limite percentual de discrepância", LOG_PREFIX);
        String value = getConfigValue("discrepancy_threshold_percentage");
        return value != null ? Integer.parseInt(value) : 20;
    }

    public int getCancellationHoursBefore() {
        log.debug("{} Obtendo horas mínimas para cancelamento", LOG_PREFIX);
        String value = getConfigValue("cancellation_hours_before");
        return value != null ? Integer.parseInt(value) : 2;
    }

    public int getAccountLockoutMaxAttempts() {
        log.debug("{} Obtendo máximo de tentativas para bloqueio de conta", LOG_PREFIX);
        String value = getConfigValue("account_lockout_max_attempts");
        return value != null ? Integer.parseInt(value) : 5;
    }

    public int getAccountLockoutDurationMinutes() {
        log.debug("{} Obtendo duração do bloqueio de conta em minutos", LOG_PREFIX);
        String value = getConfigValue("account_lockout_duration_minutes");
        return value != null ? Integer.parseInt(value) : 30;
    }

    public int getPasswordMinLength() {
        log.debug("{} Obtendo comprimento mínimo da senha", LOG_PREFIX);
        String value = getConfigValue("password_min_length");
        return value != null ? Integer.parseInt(value) : 8;
    }

    public boolean getPasswordRequireUppercase() {
        log.debug("{} Obtendo exigência de maiúscula na senha", LOG_PREFIX);
        String value = getConfigValue("password_require_uppercase");
        return value != null && Boolean.parseBoolean(value);
    }

    public boolean getPasswordRequireLowercase() {
        log.debug("{} Obtendo exigência de minúscula na senha", LOG_PREFIX);
        String value = getConfigValue("password_require_lowercase");
        return value != null && Boolean.parseBoolean(value);
    }

    public boolean getPasswordRequireNumber() {
        log.debug("{} Obtendo exigência de número na senha", LOG_PREFIX);
        String value = getConfigValue("password_require_number");
        return value != null && Boolean.parseBoolean(value);
    }

    public boolean getPasswordRequireSpecial() {
        log.debug("{} Obtendo exigência de caractere especial na senha", LOG_PREFIX);
        String value = getConfigValue("password_require_special");
        return value != null && Boolean.parseBoolean(value);
    }

    @Transactional
    public SystemConfigDTO setConfig(SystemConfigDTO request, User createdBy, String ipAddress, String userAgent) {
        log.info("{} Salvando configuração: {} = {} por usuário: {}", LOG_PREFIX, request.getConfigKey(), request.getConfigValue(), createdBy.getEmail());

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
            log.debug("{} Configuração atualizada: {}", LOG_PREFIX, savedConfig.getConfigKey());
        } else {
            auditLogService.logCreate("SYSTEM_CONFIG", savedConfig.getId(), createdBy,
                    toJson(savedConfig), ipAddress, userAgent);
            log.debug("{} Configuração criada: {}", LOG_PREFIX, savedConfig.getConfigKey());
        }

        log.info("{} Configuração do sistema salva: ID={}, Chave={}, Valor={}, Tipo={}, por={}", LOG_PREFIX, savedConfig.getId(), savedConfig.getConfigKey(), savedConfig.getConfigValue(), savedConfig.getDataType(), createdBy.getEmail());
        return SystemConfigDTO.fromEntity(savedConfig);
    }

    @Transactional
    public void deleteConfig(UUID id, User deletedBy, String ipAddress, String userAgent) {
        log.info("{} Excluindo configuração ID: {} por usuário: {}", LOG_PREFIX, id, deletedBy.getEmail());

        SystemConfig config = configRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Configuração não encontrada para exclusão: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("System Config", "id", id);
                });

        String oldValues = toJson(config);

        configRepository.delete(config);

        // Log audit
        auditLogService.logDelete("SYSTEM_CONFIG", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("{} Configuração do sistema excluída: ID={}, Chave={}, por={}", LOG_PREFIX, id, config.getConfigKey(), deletedBy.getEmail());
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