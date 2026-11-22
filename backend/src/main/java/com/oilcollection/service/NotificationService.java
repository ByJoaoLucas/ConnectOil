package com.oilcollection.service;

import com.oilcollection.dto.NotificationDTO;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.Notification;
import com.oilcollection.model.User;
import com.oilcollection.repository.NotificationRepository;
import com.oilcollection.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId, pageable)
                .map(NotificationDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderBySentAtDesc(userId)
                .stream().map(NotificationDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public Notification create(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Transactional
    public void send(UUID userId, String title, String message, String type,
                     String relatedEntityType, UUID relatedEntityId, String deliveryMethod) {
        // Load the user from database to ensure proper JPA relationship
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .deliveryMethod(deliveryMethod)
                .build();

        notificationRepository.save(notification);

        // TODO: Send via email/push/sms based on deliveryMethod
    }

    @Transactional
    public NotificationDTO markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada"));

        notification.setIsRead(true);
        notification.setReadAt(java.time.LocalDateTime.now());
        notificationRepository.save(notification);

        return NotificationDTO.fromEntity(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    // Convenience methods for common notifications
    public void notifyScheduleCreated(UUID userId, UUID scheduleId, String pointName) {
        send(userId, "Nova Coleta Agendada",
                "Sua coleta no ponto " + pointName + " foi agendada com sucesso.",
                Notification.Type.SCHEDULE_CREATED.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }

    public void notifyScheduleAssigned(UUID userId, UUID scheduleId, String collectorName) {
        send(userId, "Coleta Atribuída",
                "A coleta foi atribuída ao coletor " + collectorName + ".",
                Notification.Type.SCHEDULE_ASSIGNED.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }

    public void notifyScheduleEnRoute(UUID userId, UUID scheduleId) {
        send(userId, "Coletor a Caminho",
                "O coletor está a caminho do ponto de coleta.",
                Notification.Type.SCHEDULE_EN_ROUTE.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }

    public void notifyScheduleCompleted(UUID userId, UUID scheduleId) {
        send(userId, "Coleta Concluída",
                "A coleta foi realizada com sucesso. Comprovante disponível.",
                Notification.Type.SCHEDULE_COMPLETED.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }

    public void notifyScheduleCancelled(UUID userId, UUID scheduleId, String reason) {
        send(userId, "Coleta Cancelada",
                "A coleta foi cancelada. Motivo: " + reason,
                Notification.Type.SCHEDULE_CANCELLED.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }

    public void notifyAlmostFull(UUID userId, UUID pointId, String pointName) {
        send(userId, "Armazenamento Quase Cheio",
                "O ponto " + pointName + " está quase cheio. Priorizamos a coleta.",
                Notification.Type.COLLECTION_ALMOST_FULL.name(), "COLLECTION_POINT", pointId, "IN_APP");
    }

    public void notifyDiscrepancy(UUID userId, UUID recordId, String details) {
        send(userId, "Discrepância Detectada",
                "Foi detectada uma discrepância no volume coletado: " + details,
                Notification.Type.DISCREPANCY_DETECTED.name(), "COLLECTION_RECORD", recordId, "IN_APP");
    }

    public void notifyCertificateReady(UUID userId, UUID recordId) {
        send(userId, "Comprovante Pronto",
                "Seu comprovante de descarte está disponível para download.",
                Notification.Type.CERTIFICATE_READY.name(), "COLLECTION_RECORD", recordId, "IN_APP");
    }

    public void notifyIncidentReported(UUID userId, UUID incidentId, String type) {
        send(userId, "Incidente Reportado",
                "Um incidente do tipo " + type + " foi reportado.",
                Notification.Type.INCIDENT_REPORTED.name(), "INCIDENT", incidentId, "IN_APP");
    }

    public void notifyExchangeProductsReady(UUID userId, UUID scheduleId) {
        send(userId, "Produtos para Troca Prontos",
                "Os produtos solicitados para troca estão prontos para entrega na coleta.",
                Notification.Type.EXCHANGE_PRODUCTS_READY.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }
}