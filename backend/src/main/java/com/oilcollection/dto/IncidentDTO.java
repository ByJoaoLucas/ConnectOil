package com.oilcollection.dto;

import com.oilcollection.model.Incident;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class IncidentDTO {
    private UUID id;

    @NotNull(message = "Collection point is required")
    private UUID collectionPointId;

    private String collectionPointName;

    private UUID scheduleId;

    @NotNull(message = "Reporter is required")
    private UUID reporterId;

    private String reporterName;

    @NotBlank(message = "Incident type is required")
    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String incidentType;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Severity is required")
    @Size(max = 20, message = "Severity must not exceed 20 characters")
    private String severity;

    private String photos;

    private BigDecimal gpsLatitude;

    private BigDecimal gpsLongitude;

    private String status = "OPEN";

    private LocalDateTime resolvedAt;

    private UUID resolvedById;

    private String resolvedByName;

    private String resolutionNotes;

    private LocalDateTime reportedAt;

    private LocalDateTime updatedAt;

    public static IncidentDTO fromEntity(Incident incident) {
        IncidentDTO dto = new IncidentDTO();
        dto.setId(incident.getId());
        if (incident.getCollectionPoint() != null) {
            dto.setCollectionPointId(incident.getCollectionPoint().getId());
            dto.setCollectionPointName(incident.getCollectionPoint().getName());
        }
        if (incident.getSchedule() != null) {
            dto.setScheduleId(incident.getSchedule().getId());
        }
        if (incident.getReporter() != null) {
            dto.setReporterId(incident.getReporter().getId());
            dto.setReporterName(incident.getReporter().getName());
        }
        dto.setIncidentType(incident.getIncidentType());
        dto.setDescription(incident.getDescription());
        dto.setSeverity(incident.getSeverity());
        dto.setPhotos(incident.getPhotos());
        dto.setGpsLatitude(incident.getGpsLatitude());
        dto.setGpsLongitude(incident.getGpsLongitude());
        dto.setStatus(incident.getStatus());
        dto.setResolvedAt(incident.getResolvedAt());
        if (incident.getResolvedBy() != null) {
            dto.setResolvedById(incident.getResolvedBy().getId());
            dto.setResolvedByName(incident.getResolvedBy().getName());
        }
        dto.setResolutionNotes(incident.getResolutionNotes());
        dto.setReportedAt(incident.getReportedAt());
        dto.setUpdatedAt(incident.getUpdatedAt());
        return dto;
    }

    public boolean isOpen() {
        return "OPEN".equalsIgnoreCase(status);
    }

    public boolean isResolved() {
        return "RESOLVED".equalsIgnoreCase(status);
    }
}