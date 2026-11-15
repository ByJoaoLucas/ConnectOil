package com.oilcollection.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "collection_points", indexes = {
    @Index(name = "idx_point_status", columnList = "status"),
    @Index(name = "idx_point_collector", columnList = "collector_id"),
    @Index(name = "idx_point_location", columnList = "latitude, longitude"),
    @Index(name = "idx_point_alert_almost_full", columnList = "alert_almost_full")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointStatus status = PointStatus.ACTIVE;

    @Column(name = "capacity_liters")
    private Integer capacityLiters;

    @Column(name = "current_volume_liters")
    private Integer currentVolumeLiters = 0;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "operating_hours", columnDefinition = "JSONB")
    private String operatingHours;

    @Column(name = "accepted_oil_types", columnDefinition = "JSONB")
    private String acceptedOilTypes;

    @Column(name = "alert_almost_full", nullable = false)
    @Builder.Default
    private Boolean alertAlmostFull = false;

    @Column(name = "alert_triggered_at")
    private LocalDateTime alertTriggeredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id")
    private User collector;

    @OneToMany(mappedBy = "collectionPoint", fetch = FetchType.LAZY)
    private List<CollectionSchedule> schedules;

    @OneToMany(mappedBy = "collectionPoint", fetch = FetchType.LAZY)
    private List<CollectionRecord> collectionRecords;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PointStatus {
        ACTIVE,
        INACTIVE,
        MAINTENANCE,
        FULL,
        PENDING_APPROVAL
    }

    public Double getFillPercentage() {
        if (capacityLiters == null || capacityLiters == 0) return 0.0;
        return (currentVolumeLiters.doubleValue() / capacityLiters.doubleValue()) * 100;
    }

    public boolean isFull() {
        return getFillPercentage() >= 90;
    }
}