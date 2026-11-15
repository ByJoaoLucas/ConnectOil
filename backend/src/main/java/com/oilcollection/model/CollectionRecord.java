package com.oilcollection.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collection_records", indexes = {
    @Index(name = "idx_record_date", columnList = "collection_date"),
    @Index(name = "idx_record_point", columnList = "collection_point_id"),
    @Index(name = "idx_record_collector", columnList = "collector_id"),
    @Index(name = "idx_record_schedule", columnList = "schedule_id"),
    @Index(name = "idx_record_certificate_code", columnList = "certificate_code", unique = true),
    @Index(name = "idx_record_discrepancy", columnList = "discrepancy_flag")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_point_id", nullable = false)
    private CollectionPoint collectionPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id", nullable = false)
    private User collector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private CollectionSchedule schedule;

    @Column(name = "collection_date", nullable = false)
    private LocalDateTime collectionDate;

    @Column(name = "volume_collected_liters", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumeCollectedLiters;

    @Column(name = "oil_type", length = 50)
    private String oilType;

    @Column(name = "container_count")
    private Integer containerCount;

    @Column(name = "container_type", length = 50)
    private String containerType;

    @Column(name = "quality_rating")
    private Integer qualityRating;

    @Column(name = "temperature_celsius", precision = 5, scale = 2)
    private BigDecimal temperatureCelsius;

    @Column(name = "ph_level", precision = 3, scale = 1)
    private BigDecimal phLevel;

    @Column(name = "contamination_percentage", precision = 5, scale = 2)
    private BigDecimal contaminationPercentage;

    @Column(name = "photos", columnDefinition = "JSONB")
    private String photos;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "gps_latitude", precision = 10, scale = 8)
    private BigDecimal gpsLatitude;

    @Column(name = "gps_longitude", precision = 11, scale = 8)
    private BigDecimal gpsLongitude;

    @Column(name = "verified_by_manager", nullable = false)
    private Boolean verifiedByManager = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "actual_volume_liters", precision = 10, scale = 2)
    private BigDecimal actualVolumeLiters;

    @Column(name = "confirmation_code", length = 20)
    private String confirmationCode;

    @Column(name = "establishment_confirmed", nullable = false)
    @Builder.Default
    private Boolean establishmentConfirmed = false;

    @Column(name = "establishment_confirmed_at")
    private LocalDateTime establishmentConfirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establishment_confirmed_by")
    private User establishmentConfirmedBy;

    @Column(name = "discrepancy_flag", nullable = false)
    @Builder.Default
    private Boolean discrepancyFlag = false;

    @Column(name = "discrepancy_reason", columnDefinition = "TEXT")
    private String discrepancyReason;

    @Column(name = "certificate_code", length = 50, unique = true)
    private String certificateCode;

    @Column(name = "certificate_generated_at")
    private LocalDateTime certificateGeneratedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isHighQuality() {
        return qualityRating != null && qualityRating >= 4;
    }

    public boolean hasContamination() {
        return contaminationPercentage != null && contaminationPercentage.compareTo(BigDecimal.ZERO) > 0;
    }
}