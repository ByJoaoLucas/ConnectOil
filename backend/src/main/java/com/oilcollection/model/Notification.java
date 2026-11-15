package com.oilcollection.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "user_id, is_read"),
    @Index(name = "idx_notification_sent_at", columnList = "sent_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime sentAt;

    @Column(name = "delivery_method", nullable = false, length = 20)
    @Builder.Default
    private String deliveryMethod = "IN_APP";

    public enum Type {
        SCHEDULE_CREATED,
        SCHEDULE_ASSIGNED,
        SCHEDULE_EN_ROUTE,
        SCHEDULE_COMPLETED,
        SCHEDULE_CANCELLED,
        COLLECTION_ALMOST_FULL,
        DISCREPANCY_DETECTED,
        CERTIFICATE_READY,
        INCIDENT_REPORTED,
        EXCHANGE_PRODUCTS_READY
    }

    public enum DeliveryMethod {
        IN_APP,
        EMAIL,
        PUSH,
        SMS
    }
}