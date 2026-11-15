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

    @Transactional(readOnly = true)
    public Page<IncidentDTO> getAllIncidents(Pageable pageable) {
        return incidentRepository.findAll(pageable).map(IncidentDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public IncidentDTO getIncidentById(UUID id) {
        return incidentRepository.findById(id)
                .map(IncidentDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> getIncidentsByPoint(UUID pointId) {
        return incidentRepository.findByCollectionPointId(pointId).stream()
                .map(IncidentDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> getIncidentsBySchedule(UUID scheduleId) {
        return incidentRepository.findByScheduleId(scheduleId).stream()
                .map(IncidentDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> getIncidentsByReporter(UUID reporterId) {
        return incidentRepository.findByReporterId(reporterId).stream()
                .map(IncidentDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<IncidentDTO> getIncidentsByStatus(String status, Pageable pageable) {
        return incidentRepository.findByStatus(status, pageable).map(IncidentDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> getRecentIncidentsByPoint(UUID pointId, LocalDateTime since) {
        return incidentRepository.findRecentByCollectionPoint(pointId, since).stream()
                .map(IncidentDTO::fromEntity).toList();
    }

    @Transactional
    public IncidentDTO createIncident(IncidentDTO request, User createdBy, String ipAddress, String userAgent) {
        Incident incident = new Incident();

        if (request.getCollectionPointId() != null) {
            CollectionPoint point = pointRepository.findById(request.getCollectionPointId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", request.getCollectionPointId()));
            incident.setCollectionPoint(point);
        }

        if (request.getScheduleId() != null) {
            CollectionSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", request.getScheduleId()));
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

        log.info("Incident created: {} by {}", savedIncident.getId(), createdBy.getEmail());
        return IncidentDTO.fromEntity(savedIncident);
    }

    @Transactional
    public IncidentDTO updateIncident(UUID id, IncidentDTO request, User updatedBy, String ipAddress, String userAgent) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

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

        log.info("Incident updated: {} by {}", id, updatedBy.getEmail());
        return IncidentDTO.fromEntity(updatedIncident);
    }

    @Transactional
    public IncidentDTO resolveIncident(UUID id, User resolvedBy, String resolutionNotes, String ipAddress, String userAgent) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        String oldValues = toJson(incident);

        incident.setStatus("RESOLVED");
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolvedBy(resolvedBy);
        incident.setResolutionNotes(resolutionNotes);

        Incident resolvedIncident = incidentRepository.save(incident);

        // Log audit
        auditLogService.logStatusChange("INCIDENT", resolvedIncident.getId(), resolvedBy,
                oldValues, toJson(resolvedIncident), ipAddress, userAgent);

        log.info("Incident resolved: {} by {}", id, resolvedBy.getEmail());
        return IncidentDTO.fromEntity(resolvedIncident);
    }

    @Transactional
    public void deleteIncident(UUID id, User deletedBy, String ipAddress, String userAgent) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        String oldValues = toJson(incident);

        incidentRepository.delete(incident);

        // Log audit
        auditLogService.logDelete("INCIDENT", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("Incident deleted: {} by {}", id, deletedBy.getEmail());
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