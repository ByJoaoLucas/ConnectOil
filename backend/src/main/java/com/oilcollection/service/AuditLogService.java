package com.oilcollection.service;

import com.oilcollection.model.AuditLog;
import com.oilcollection.model.User;
import com.oilcollection.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    private static final String LOG_PREFIX = "[AuditLogService]";

    public List<AuditLog> findByEntity(String entityType, UUID entityId) {
        log.debug("{} Buscando logs de auditoria para entidade: {} ID: {}", LOG_PREFIX, entityType, entityId);
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public Page<AuditLog> findByEntity(String entityType, UUID entityId, Pageable pageable) {
        log.debug("{} Buscando logs de auditoria para entidade: {} ID: {} com paginação: {}", LOG_PREFIX, entityType, entityId, pageable);
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    public List<AuditLog> findByUser(UUID userId) {
        log.debug("{} Buscando logs de auditoria por usuário ID: {}", LOG_PREFIX, userId);
        return auditLogRepository.findByChangedById(userId);
    }

    public List<AuditLog> findRecent(LocalDateTime since) {
        log.debug("{} Buscando logs de auditoria recentes desde: {}", LOG_PREFIX, since);
        return auditLogRepository.findRecent(since);
    }

    public void log(String entityType, UUID entityId, String action, User changedBy,
                    String oldValues, String newValues, String ipAddress, String userAgent) {
        log.debug("{} Registrando log de auditoria: Entidade={}, ID={}, Ação={}, Usuário={}, IP={}",
                LOG_PREFIX, entityType, entityId, action, changedBy.getEmail(), ipAddress);

        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .changedBy(changedBy)
                .oldValues(oldValues)
                .newValues(newValues)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("{} Log de auditoria salvo com ID: {}", LOG_PREFIX, auditLog.getId());
    }

    public void logCreate(String entityType, UUID entityId, User changedBy,
                          String newValues, String ipAddress, String userAgent) {
        log.debug("{} Log CREATE: Entidade={}, ID={}, Usuário={}", LOG_PREFIX, entityType, entityId, changedBy.getEmail());
        log(entityType, entityId, AuditLog.Action.CREATE.name(), changedBy, null, newValues, ipAddress, userAgent);
    }

    public void logUpdate(String entityType, UUID entityId, User changedBy,
                          String oldValues, String newValues, String ipAddress, String userAgent) {
        log.debug("{} Log UPDATE: Entidade={}, ID={}, Usuário={}", LOG_PREFIX, entityType, entityId, changedBy.getEmail());
        log(entityType, entityId, AuditLog.Action.UPDATE.name(), changedBy, oldValues, newValues, ipAddress, userAgent);
    }

    public void logDelete(String entityType, UUID entityId, User changedBy,
                          String oldValues, String ipAddress, String userAgent) {
        log.debug("{} Log DELETE: Entidade={}, ID={}, Usuário={}", LOG_PREFIX, entityType, entityId, changedBy.getEmail());
        log(entityType, entityId, AuditLog.Action.DELETE.name(), changedBy, oldValues, null, ipAddress, userAgent);
    }

    public void logStatusChange(String entityType, UUID entityId, User changedBy,
                                String oldStatus, String newStatus, String ipAddress, String userAgent) {
        log.debug("{} Log STATUS_CHANGE: Entidade={}, ID={}, Usuário={}, StatusAntigo={}, StatusNovo={}",
                LOG_PREFIX, entityType, entityId, changedBy.getEmail(), oldStatus, newStatus);
        String oldValues = "{\"status\": \"" + oldStatus + "\"}";
        String newValues = "{\"status\": \"" + newStatus + "\"}";
        log(entityType, entityId, AuditLog.Action.STATUS_CHANGE.name(), changedBy, oldValues, newValues, ipAddress, userAgent);
    }
}