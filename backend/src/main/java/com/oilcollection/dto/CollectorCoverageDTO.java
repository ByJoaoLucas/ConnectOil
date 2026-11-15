package com.oilcollection.dto;

import com.oilcollection.model.CollectorCoverage;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CollectorCoverageDTO {
    private UUID id;

    @NotNull(message = "Collector is required")
    private UUID collectorId;

    private String collectorName;

    @NotNull(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotNull(message = "State is required")
    @Size(min = 2, max = 2, message = "State must be 2 characters")
    private String state;

    @Size(max = 500, message = "Neighborhoods JSON must not exceed 500 characters")
    private String neighborhoods;

    private BigDecimal maxDistanceKm;

    private Boolean isActive = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static CollectorCoverageDTO fromEntity(CollectorCoverage coverage) {
        CollectorCoverageDTO dto = new CollectorCoverageDTO();
        dto.setId(coverage.getId());
        if (coverage.getCollector() != null) {
            dto.setCollectorId(coverage.getCollector().getId());
            dto.setCollectorName(coverage.getCollector().getName());
        }
        dto.setCity(coverage.getCity());
        dto.setState(coverage.getState());
        dto.setNeighborhoods(coverage.getNeighborhoods());
        dto.setMaxDistanceKm(coverage.getMaxDistanceKm());
        dto.setIsActive(coverage.getIsActive());
        dto.setCreatedAt(coverage.getCreatedAt());
        dto.setUpdatedAt(coverage.getUpdatedAt());
        return dto;
    }
}