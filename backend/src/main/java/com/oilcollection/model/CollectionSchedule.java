package com.oilcollection.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "collection_schedules", indexes = {
    @Index(name = "idx_schedule_date", columnList = "scheduled_date"),
    @Index(name = "idx_schedule_collector", columnList = "collector_id"),
    @Index(name = "idx_schedule_point", columnList = "collection_point_id"),
    @Index(name = "idx_schedule_status", columnList = "status"),
    @Index(name = "idx_schedule_sale_type", columnList = "sale_type"),
    @Index(name = "idx_schedule_assigned_at", columnList = "assigned_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_point_id", nullable = false)
    private CollectionPoint collectionPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id", nullable = false)
    private User collector;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status = ScheduleStatus.PENDING;

    @Column(name = "estimated_volume_liters")
    private Integer estimatedVolumeLiters;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false)
    @Builder.Default
    private SaleType saleType = SaleType.SALE;

    @Column(name = "time_window", length = 50)
    private String timeWindow;

    @Column(name = "base_price_per_liter", precision = 10, scale = 2)
    private BigDecimal basePricePerLiter;

    @Column(name = "calculated_value", precision = 10, scale = 2)
    private BigDecimal calculatedValue;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "en_route_at")
    private LocalDateTime enRouteAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ScheduleStatus {
        PENDING,
        CONFIRMED,
        ASSIGNED,
        EN_ROUTE,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        RESCHEDULED
    }

    public enum SaleType {
        SALE,
        EXCHANGE
    }

    public boolean isOverdue() {
        return status == ScheduleStatus.PENDING && scheduledDate.isBefore(LocalDate.now());
    }

    public boolean isToday() {
        return scheduledDate.equals(LocalDate.now());
    }
}