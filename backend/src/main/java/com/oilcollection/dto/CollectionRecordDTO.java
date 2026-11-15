package com.oilcollection.dto;

import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.CollectionRecord;
import com.oilcollection.model.CollectionSchedule;
import com.oilcollection.model.User;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CollectionRecordDTO {
    private UUID id;

    @NotNull(message = "Collection point is required")
    private UUID collectionPointId;

    private String collectionPointName;

    @NotNull(message = "Collector is required")
    private UUID collectorId;

    private String collectorName;

    private UUID scheduleId;

    @NotNull(message = "Collection date is required")
    @PastOrPresent(message = "Collection date cannot be in the future")
    private LocalDateTime collectionDate;

    @NotNull(message = "Volume collected is required")
    @DecimalMin(value = "0.01", message = "Volume must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Invalid volume format")
    private BigDecimal volumeCollectedLiters;

    @Size(max = 50, message = "Oil type must not exceed 50 characters")
    private String oilType;

    @Min(value = 1, message = "Container count must be at least 1")
    private Integer containerCount;

    @Size(max = 50, message = "Container type must not exceed 50 characters")
    private String containerType;

    @Min(value = 1, message = "Quality rating must be at least 1")
    @Max(value = 5, message = "Quality rating must not exceed 5")
    private Integer qualityRating;

    @DecimalMin(value = "-50.0", message = "Temperature must be at least -50°C")
    @DecimalMax(value = "200.0", message = "Temperature must not exceed 200°C")
    private BigDecimal temperatureCelsius;

    @DecimalMin(value = "0.0", message = "pH level must be at least 0")
    @DecimalMax(value = "14.0", message = "pH level must not exceed 14")
    private BigDecimal phLevel;

    @DecimalMin(value = "0.0", message = "Contamination percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Contamination percentage cannot exceed 100%")
    private BigDecimal contaminationPercentage;

    private String photos;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal gpsLatitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal gpsLongitude;

    private Boolean verifiedByManager = false;

    private LocalDateTime verifiedAt;

    private UUID verifiedById;

    private String verifiedByName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // US05, US14 - Confirmation and certificate fields
    private String confirmationCode;
    private Boolean establishmentConfirmed = false;
    private LocalDateTime establishmentConfirmedAt;
    private UUID establishmentConfirmedById;
    private Boolean discrepancyFlag = false;
    private String discrepancyDetails;
    private String certificateCode;

    public static CollectionRecordDTO fromEntity(CollectionRecord record) {
        CollectionRecordDTO dto = new CollectionRecordDTO();
        dto.setId(record.getId());
        if (record.getCollectionPoint() != null) {
            dto.setCollectionPointId(record.getCollectionPoint().getId());
            dto.setCollectionPointName(record.getCollectionPoint().getName());
        }
        if (record.getCollector() != null) {
            dto.setCollectorId(record.getCollector().getId());
            dto.setCollectorName(record.getCollector().getName());
        }
        if (record.getSchedule() != null) {
            dto.setScheduleId(record.getSchedule().getId());
        }
        dto.setCollectionDate(record.getCollectionDate());
        dto.setVolumeCollectedLiters(record.getVolumeCollectedLiters());
        dto.setOilType(record.getOilType());
        dto.setContainerCount(record.getContainerCount());
        dto.setContainerType(record.getContainerType());
        dto.setQualityRating(record.getQualityRating());
        dto.setTemperatureCelsius(record.getTemperatureCelsius());
        dto.setPhLevel(record.getPhLevel());
        dto.setContaminationPercentage(record.getContaminationPercentage());
        dto.setPhotos(record.getPhotos());
        dto.setNotes(record.getNotes());
        dto.setGpsLatitude(record.getGpsLatitude());
        dto.setGpsLongitude(record.getGpsLongitude());
        dto.setVerifiedByManager(record.getVerifiedByManager());
        dto.setVerifiedAt(record.getVerifiedAt());
        if (record.getVerifiedBy() != null) {
            dto.setVerifiedById(record.getVerifiedBy().getId());
            dto.setVerifiedByName(record.getVerifiedBy().getName());
        }
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        dto.setConfirmationCode(record.getConfirmationCode());
        dto.setEstablishmentConfirmed(record.getEstablishmentConfirmed());
        dto.setEstablishmentConfirmedAt(record.getEstablishmentConfirmedAt());
        dto.setDiscrepancyFlag(record.getDiscrepancyFlag());
        dto.setDiscrepancyDetails(record.getDiscrepancyReason());
        dto.setCertificateCode(record.getCertificateCode());
        return dto;
    }

    public boolean isHighQuality() {
        return qualityRating != null && qualityRating >= 4;
    }

    public boolean hasContamination() {
        return contaminationPercentage != null && contaminationPercentage.compareTo(BigDecimal.ZERO) > 0;
    }
}