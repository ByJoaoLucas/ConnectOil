package com.oilcollection.service;

import com.oilcollection.dto.NotificationDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.Notification;
import com.oilcollection.model.User;
import com.oilcollection.repository.NotificationRepository;
import com.oilcollection.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final String LOG_PREFIX = "[NotificationService]";

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(UUID userId, Pageable pageable) {
        log.debug("{} Buscando notificações para usuário: {} com paginação: {}", LOG_PREFIX, userId, pageable);
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId, pageable)
                .map(NotificationDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(UUID userId) {
        log.debug("{} Buscando notificações não lidas para usuário: {}", LOG_PREFIX, userId);
        return notificationRepository.findByUserIdAndIsReadFalseOrderBySentAtDesc(userId)
                .stream().map(NotificationDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        log.debug("{} Contando notificações não lidas para usuário: {}", LOG_PREFIX, userId);
        return notificationRepository.countUnreadByUserId(userId);
    }

    public Notification create(Notification notification) {
        log.debug("{} Criando notificação direta para usuário: {}", LOG_PREFIX, notification.getUser().getId());
        return notificationRepository.save(notification);
    }

    @Transactional
    public void send(UUID userId, String title, String message, String type,
                     String relatedEntityType, UUID relatedEntityId, String deliveryMethod) {
        log.debug("{} Enviando notificação para usuário: {} - Título: {}, Tipo: {}", LOG_PREFIX, userId, title, type);

        // Load the user from database to ensure proper JPA relationship
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("{} Usuário não encontrado para notificação: {}", LOG_PREFIX, userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });

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

        log.info("{} Notificação enviada: ID={}, Para={}, Tipo={}", LOG_PREFIX, notification.getId(), user.getEmail(), type);

        // TODO: Send via email/push/sms based on deliveryMethod
    }

    @Transactional
    public NotificationDTO markAsRead(UUID notificationId) {
        log.debug("{} Marcando notificação como lida: {}", LOG_PREFIX, notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.warn("{} Notificação não encontrada para marcar como lida: {}", LOG_PREFIX, notificationId);
                    return new IllegalArgumentException("Notificação não encontrada");
                });

        notification.setIsRead(true);
        notification.setReadAt(java.time.LocalDateTime.now());
        notificationRepository.save(notification);

        log.info("{} Notificação marcada como lida: ID={}, Usuário={}", LOG_PREFIX, notificationId, notification.getUser().getEmail());
        return NotificationDTO.fromEntity(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        log.debug("{} Marcando todas as notificações como lidas para usuário: {}", LOG_PREFIX, userId);
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("{} Todas as notificações marcadas como lidas para usuário: {}", LOG_PREFIX, userId);
    }

    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        log.debug("{} Excluindo notificação ID: {} para usuário: {}", LOG_PREFIX, notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.warn("{} Notificação não encontrada para exclusão: {}", LOG_PREFIX, notificationId);
                    return new ResourceNotFoundException("Notificação", "id", notificationId);
                });

        if (!notification.getUser().getId().equals(userId)) {
            log.warn("{} Usuário {} tentou excluir notificação de outro usuário: {}", LOG_PREFIX, userId, notificationId);
            throw new BusinessException("Notificação não pertence ao usuário");
        }

        if (!notification.getIsRead()) {
            log.warn("{} Tentativa de excluir notificação não lida: {}", LOG_PREFIX, notificationId);
            throw new BusinessException("Apenas notificações lidas podem ser excluídas");
        }

        notificationRepository.delete(notification);

        log.info("{} Notificação excluída: ID={}, Usuário={}", LOG_PREFIX, notificationId, userId);
    }

    // Convenience methods for common notifications
    public void notifyScheduleCreated(UUID userId, UUID scheduleId, String pointName) {
        log.debug("{} Notificando agendamento criado para usuário: {}, Agendamento: {}, Ponto: {}", LOG_PREFIX, userId, scheduleId, pointName);
        send(userId, "Nova Coleta Agendada",
                "Sua coleta no ponto " + pointName + " foi agendada com sucesso.",
                Notification.Type.SCHEDULE_CREATED.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }

    public void notifyScheduleAssigned(UUID userId, UUID scheduleId, String collectorName) {
        log.debug("{} Notificando agendamento atribuído para usuário: {}, Agendamento: {}, Coletor: {}", LOG_PREFIX, userId, scheduleId, collectorName);
        send(userId, "Coleta Atribuída",
                "A coleta foi atribuída ao coletor " + collectorName + ".",
                Notification.Type.SCHEDULE_ASSIGNED.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }

    public void notifyScheduleEnRoute(UUID userId, UUID scheduleId) {
        log.debug("{} Notificando coletor a caminho para usuário: {}, Agendamento: {}", LOG_PREFIX, userId, scheduleId);
        send(userId, "Coletor a Caminho",
                "O coletor está a caminho do ponto de coleta.",
                Notification.Type.SCHEDULE_EN_ROUTE.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }

    public void notifyScheduleCompleted(UUID userId, UUID scheduleId) {
        log.debug("{} Notificando agendamento concluído para usuário: {}, Agendamento: {}", LOG_PREFIX, userId, scheduleId);
        send(userId, "Coleta Concluída",
                "A coleta foi realizada com sucesso. Comprovante disponível.",
                Notification.Type.SCHEDULE_COMPLETED.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }

    public void notifyScheduleCancelled(UUID userId, UUID scheduleId, String reason) {
        log.debug("{} Notificando agendamento cancelado para usuário: {}, Agendamento: {}, Motivo: {}", LOG_PREFIX, userId, scheduleId, reason);
        send(userId, "Coleta Cancelada",
                "A coleta foi cancelada. Motivo: " + reason,
                Notification.Type.SCHEDULE_CANCELLED.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }

    public void notifyAlmostFull(UUID userId, UUID pointId, String pointName) {
        log.debug("{} Notificando ponto quase cheio para usuário: {}, Ponto: {}, Nome: {}", LOG_PREFIX, userId, pointId, pointName);
        send(userId, "Armazenamento Quase Cheio",
                "O ponto " + pointName + " está quase cheio. Priorizamos a coleta.",
                Notification.Type.COLLECTION_ALMOST_FULL.name(), "COLLECTION_POINT", pointId, "IN_APP");
    }

    public void notifyDiscrepancy(UUID userId, UUID recordId, String details) {
        log.debug("{} Notificando discrepância para usuário: {}, Registro: {}, Detalhes: {}", LOG_PREFIX, userId, recordId, details);
        send(userId, "Discrepância Detectada",
                "Foi detectada uma discrepância no volume coletado: " + details,
                Notification.Type.DISCREPANCY_DETECTED.name(), "COLLECTION_RECORD", recordId, "IN_APP");
    }

    public void notifyCertificateReady(UUID userId, UUID recordId) {
        log.debug("{} Notificando comprovante pronto para usuário: {}, Registro: {}", LOG_PREFIX, userId, recordId);
        send(userId, "Comprovante Pronto",
                "Seu comprovante de descarte está disponível para download.",
                Notification.Type.CERTIFICATE_READY.name(), "COLLECTION_RECORD", recordId, "IN_APP");
    }

    public void notifyIncidentReported(UUID userId, UUID incidentId, String type) {
        log.debug("{} Notificando incidente reportado para usuário: {}, Incidente: {}, Tipo: {}", LOG_PREFIX, userId, incidentId, type);
        send(userId, "Incidente Reportado",
                "Um incidente do tipo " + type + " foi reportado.",
                Notification.Type.INCIDENT_REPORTED.name(), "INCIDENT", incidentId, "IN_APP");
    }

    public void notifyExchangeProductsReady(UUID userId, UUID scheduleId) {
        log.debug("{} Notificando produtos para troca prontos para usuário: {}, Agendamento: {}", LOG_PREFIX, userId, scheduleId);
        send(userId, "Produtos para Troca Prontos",
                "Os produtos solicitados para troca estão prontos para entrega na coleta.",
                Notification.Type.EXCHANGE_PRODUCTS_READY.name(), "COLLECTION_SCHEDULE", scheduleId, "IN_APP");
    }
}