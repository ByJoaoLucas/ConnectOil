package com.oilcollection.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectorPerformanceDTO {
    private UUID collectorId;
    private String collectorName;
    private Long totalCollections;
    private BigDecimal totalVolume;
    private Double averageQuality;
}