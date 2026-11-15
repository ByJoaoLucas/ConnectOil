package com.oilcollection.dto;

import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.CollectionSchedule;
import com.oilcollection.model.User;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class CollectionScheduleDTO {
    private UUID id;

    @NotNull(message = "Collection point is required")
    private UUID collectionPointId;

    private String collectionPointName;

    @NotNull(message = "Collector is required")
    private UUID collectorId;

    private String collectorName;

    @NotNull(message = "Scheduled date is required")
    @FutureOrPresent(message = "Scheduled date must be today or in the future")
    private LocalDate scheduledDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    private LocalTime endTime;

    private CollectionSchedule.ScheduleStatus status = CollectionSchedule.ScheduleStatus.PENDING;

    @Min(value = 1, message = "Estimated volume must be at least 1 liter")
    private Integer estimatedVolumeLiters;

    private String notes;

    // US02, US06 - Sale/Exchange type and time window
    private CollectionSchedule.SaleType saleType = CollectionSchedule.SaleType.SALE;

    private String timeWindow;

    private BigDecimal basePricePerLiter;

    private BigDecimal calculatedValue;

    // US03 - Status tracking
    private LocalDateTime assignedAt;

    private LocalDateTime enRouteAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private String cancellationReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // US06 - Exchange items
    private java.util.List<ExchangeItemDTO> exchangeItems;

    public static CollectionScheduleDTO fromEntity(CollectionSchedule schedule) {
        CollectionScheduleDTO dto = new CollectionScheduleDTO();
        dto.setId(schedule.getId());
        if (schedule.getCollectionPoint() != null) {
            dto.setCollectionPointId(schedule.getCollectionPoint().getId());
            dto.setCollectionPointName(schedule.getCollectionPoint().getName());
        }
        if (schedule.getCollector() != null) {
            dto.setCollectorId(schedule.getCollector().getId());
            dto.setCollectorName(schedule.getCollector().getName());
        }
        dto.setScheduledDate(schedule.getScheduledDate());
        dto.setStartTime(schedule.getStartTime());
        dto.setEndTime(schedule.getEndTime());
        dto.setStatus(schedule.getStatus());
        dto.setEstimatedVolumeLiters(schedule.getEstimatedVolumeLiters());
        dto.setNotes(schedule.getNotes());
        dto.setSaleType(schedule.getSaleType());
        dto.setTimeWindow(schedule.getTimeWindow());
        dto.setBasePricePerLiter(schedule.getBasePricePerLiter());
        dto.setCalculatedValue(schedule.getCalculatedValue());
        dto.setAssignedAt(schedule.getAssignedAt());
        dto.setEnRouteAt(schedule.getEnRouteAt());
        dto.setCompletedAt(schedule.getCompletedAt());
        dto.setCancelledAt(schedule.getCancelledAt());
        dto.setCancellationReason(schedule.getCancellationReason());
        dto.setCreatedAt(schedule.getCreatedAt());
        dto.setUpdatedAt(schedule.getUpdatedAt());
        return dto;
    }

    public boolean isOverdue() {
        return status == CollectionSchedule.ScheduleStatus.PENDING && scheduledDate.isBefore(LocalDate.now());
    }

    public boolean isToday() {
        return scheduledDate.equals(LocalDate.now());
    }

    @Data
    public static class ExchangeItemDTO {
        private UUID productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}