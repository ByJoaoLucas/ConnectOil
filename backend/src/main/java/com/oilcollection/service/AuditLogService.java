package com.oilcollection.service;

import com.oilcollection.model.AuditLog;
import com.oilcollection.model.User;
import com.oilcollection.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
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
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public List<AuditLog> findByEntity(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public Page<AuditLog> findByEntity(String entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    public List<AuditLog> findByUser(UUID userId) {
        return auditLogRepository.findByChangedBy(userId);
    }

    public List<AuditLog> findRecent(LocalDateTime since) {
        return auditLogRepository.findRecent(since);
    }

    public void log(String entityType, UUID entityId, String action, User changedBy,
                    String oldValues, String newValues, String ipAddress, String userAgent) {
        AuditLog log = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .changedBy(changedBy)
                .oldValues(oldValues)
                .newValues(newValues)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(log);
    }

    public void logCreate(String entityType, UUID entityId, User changedBy,
                          String newValues, String ipAddress, String userAgent) {
        log(entityType, entityId, AuditLog.Action.CREATE.name(), changedBy, null, newValues, ipAddress, userAgent);
    }

    public void logUpdate(String entityType, UUID entityId, User changedBy,
                          String oldValues, String newValues, String ipAddress, String userAgent) {
        log(entityType, entityId, AuditLog.Action.UPDATE.name(), changedBy, oldValues, newValues, ipAddress, userAgent);
    }

    public void logDelete(String entityType, UUID entityId, User changedBy,
                          String oldValues, String ipAddress, String userAgent) {
        log(entityType, entityId, AuditLog.Action.DELETE.name(), changedBy, oldValues, null, ipAddress, userAgent);
    }

    public void logStatusChange(String entityType, UUID entityId, User changedBy,
                                String oldStatus, String newStatus, String ipAddress, String userAgent) {
        String oldValues = "{\"status\": \"" + oldStatus + "\"}";
        String newValues = "{\"status\": \"" + newStatus + "\"}";
        log(entityType, entityId, AuditLog.Action.STATUS_CHANGE.name(), changedBy, oldValues, newValues, ipAddress, userAgent);
    }
}