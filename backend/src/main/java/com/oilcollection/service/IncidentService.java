package com.oilcollection.service;

import com.oilcollection.dto.IncidentDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.CollectionSchedule;
import com.oilcollection.model.Incident;
import com.oilcollection.model.User;
import com.oilcollection.repository.CollectionPointRepository;
import com.oilcollection.repository.CollectionScheduleRepository;
import com.oilcollection.repository.IncidentRepository;
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
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final CollectionPointRepository pointRepository;
    private final CollectionScheduleRepository scheduleRepository;
    private final AuditLogService auditLogService;

    private static final String LOG_PREFIX = "[IncidentService]";

    @Transactional(readOnly = true)
    public Page<IncidentDTO> getAllIncidents(Pageable pageable) {
        log.debug("{} Buscando todos os incidentes com paginação: {}", LOG_PREFIX, pageable);
        return incidentRepository.findAll(pageable).map(IncidentDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public IncidentDTO getIncidentById(UUID id) {
        log.debug("{} Buscando incidente por ID: {}", LOG_PREFIX, id);
        return incidentRepository.findById(id)
                .map(IncidentDTO::fromEntity)
                .orElseThrow(() -> {
                    log.warn("{} Incidente não encontrado: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Incident", "id", id);
                });
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> getIncidentsByPoint(UUID pointId) {
        log.debug("{} Buscando incidentes por ponto ID: {}", LOG_PREFIX, pointId);
        return incidentRepository.findByCollectionPointId(pointId).stream()
                .map(IncidentDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> getIncidentsBySchedule(UUID scheduleId) {
        log.debug("{} Buscando incidentes por agendamento ID: {}", LOG_PREFIX, scheduleId);
        return incidentRepository.findByScheduleId(scheduleId).stream()
                .map(IncidentDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> getIncidentsByReporter(UUID reporterId) {
        log.debug("{} Buscando incidentes por relator ID: {}", LOG_PREFIX, reporterId);
        return incidentRepository.findByReporterId(reporterId).stream()
                .map(IncidentDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<IncidentDTO> getIncidentsByStatus(String status, Pageable pageable) {
        log.debug("{} Buscando incidentes por status: {} com paginação: {}", LOG_PREFIX, status, pageable);
        return incidentRepository.findByStatus(status, pageable).map(IncidentDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> getRecentIncidentsByPoint(UUID pointId, LocalDateTime since) {
        log.debug("{} Buscando incidentes recentes por ponto ID: {} desde: {}", LOG_PREFIX, pointId, since);
        return incidentRepository.findRecentByCollectionPoint(pointId, since).stream()
                .map(IncidentDTO::fromEntity).toList();
    }

    @Transactional
    public IncidentDTO createIncident(IncidentDTO request, User createdBy, String ipAddress, String userAgent) {
        log.info("{} Criando incidente do tipo: {} por usuário: {}", LOG_PREFIX, request.getIncidentType(), createdBy.getEmail());

        Incident incident = new Incident();

        if (request.getCollectionPointId() != null) {
            CollectionPoint point = pointRepository.findById(request.getCollectionPointId())
                    .orElseThrow(() -> {
                        log.warn("{} Ponto de coleta não encontrado para incidente: {}", LOG_PREFIX, request.getCollectionPointId());
                        return new ResourceNotFoundException("Collection Point", "id", request.getCollectionPointId());
                    });
            incident.setCollectionPoint(point);
        }

        if (request.getScheduleId() != null) {
            CollectionSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                    .orElseThrow(() -> {
                        log.warn("{} Agendamento não encontrado para incidente: {}", LOG_PREFIX, request.getScheduleId());
                        return new ResourceNotFoundException("Schedule", "id", request.getScheduleId());
                    });
            incident.setSchedule(schedule);
        }

        incident.setReporter(createdBy);
        incident.setIncidentType(request.getIncidentType());
        incident.setDescription(request.getDescription());
        incident.setSeverity(request.getSeverity());
        incident.setStatus("OPEN");
        incident.setPhotos(request.getPhotos());
        incident.setGpsLatitude(request.getGpsLatitude());
        incident.setGpsLongitude(request.getGpsLongitude());

        Incident savedIncident = incidentRepository.save(incident);

        // Log audit
        auditLogService.logCreate("INCIDENT", savedIncident.getId(), createdBy,
                toJson(savedIncident), ipAddress, userAgent);

        log.info("{} Incidente criado: ID={}, Tipo={}, Severidade={}, por={}", LOG_PREFIX, savedIncident.getId(), savedIncident.getIncidentType(), savedIncident.getSeverity(), createdBy.getEmail());
        return IncidentDTO.fromEntity(savedIncident);
    }

    @Transactional
    public IncidentDTO updateIncident(UUID id, IncidentDTO request, User updatedBy, String ipAddress, String userAgent) {
        log.info("{} Atualizando incidente ID: {} por usuário: {}", LOG_PREFIX, id, updatedBy.getEmail());

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Incidente não encontrado para atualização: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Incident", "id", id);
                });

        String oldValues = toJson(incident);

        incident.setIncidentType(request.getIncidentType());
        incident.setDescription(request.getDescription());
        incident.setSeverity(request.getSeverity());
        incident.setPhotos(request.getPhotos());
        incident.setGpsLatitude(request.getGpsLatitude());
        incident.setGpsLongitude(request.getGpsLongitude());

        Incident updatedIncident = incidentRepository.save(incident);

        // Log audit
        auditLogService.logUpdate("INCIDENT", updatedIncident.getId(), updatedBy,
                oldValues, toJson(updatedIncident), ipAddress, userAgent);

        log.info("{} Incidente atualizado: ID={}, Tipo={}, Severidade={}, por={}", LOG_PREFIX, id, updatedIncident.getIncidentType(), updatedIncident.getSeverity(), updatedBy.getEmail());
        return IncidentDTO.fromEntity(updatedIncident);
    }

    @Transactional
    public IncidentDTO resolveIncident(UUID id, User resolvedBy, String resolutionNotes, String ipAddress, String userAgent) {
        log.info("{} Resolvendo incidente ID: {} por usuário: {}", LOG_PREFIX, id, resolvedBy.getEmail());

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Incidente não encontrado para resolução: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Incident", "id", id);
                });

        String oldValues = toJson(incident);

        incident.setStatus("RESOLVED");
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolvedBy(resolvedBy);
        incident.setResolutionNotes(resolutionNotes);

        Incident resolvedIncident = incidentRepository.save(incident);

        // Log audit
        auditLogService.logStatusChange("INCIDENT", resolvedIncident.getId(), resolvedBy,
                oldValues, toJson(resolvedIncident), ipAddress, userAgent);

        log.info("{} Incidente resolvido: ID={}, por={}, Notas={}", LOG_PREFIX, id, resolvedBy.getEmail(), resolutionNotes);
        return IncidentDTO.fromEntity(resolvedIncident);
    }

    @Transactional
    public void deleteIncident(UUID id, User deletedBy, String ipAddress, String userAgent) {
        log.info("{} Excluindo incidente ID: {} por usuário: {}", LOG_PREFIX, id, deletedBy.getEmail());

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Incidente não encontrado para exclusão: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Incident", "id", id);
                });

        String oldValues = toJson(incident);

        incidentRepository.delete(incident);

        // Log audit
        auditLogService.logDelete("INCIDENT", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("{} Incidente excluído: ID={}, por={}", LOG_PREFIX, id, deletedBy.getEmail());
    }

    private String toJson(Incident incident) {
        return String.format(
                "{\"id\":\"%s\",\"type\":\"%s\",\"severity\":\"%s\",\"status\":\"%s\",\"point\":\"%s\"}",
                incident.getId(),
                incident.getIncidentType(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getCollectionPoint() != null ? incident.getCollectionPoint().getName() : "null"
        );
    }
}