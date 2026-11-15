package com.oilcollection.dto;

import com.oilcollection.model.Notification;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NotificationDTO {
    private UUID id;

    private UUID userId;

    private String userName;

    private String title;

    private String message;

    private String type;

    private String relatedEntityType;

    private UUID relatedEntityId;

    private String deliveryMethod;

    private Boolean isRead = false;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    public static NotificationDTO fromEntity(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        if (notification.getUser() != null) {
            dto.setUserId(notification.getUser().getId());
            dto.setUserName(notification.getUser().getName());
        }
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setRelatedEntityType(notification.getRelatedEntityType());
        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setDeliveryMethod(notification.getDeliveryMethod());
        dto.setIsRead(notification.getIsRead());
        dto.setSentAt(notification.getSentAt());
        dto.setReadAt(notification.getReadAt());
        return dto;
    }
}