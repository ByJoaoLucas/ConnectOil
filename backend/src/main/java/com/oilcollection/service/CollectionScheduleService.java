package com.oilcollection.service;

import com.oilcollection.dto.CollectionScheduleDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.CollectionSchedule;
import com.oilcollection.model.User;
import com.oilcollection.repository.CollectionPointRepository;
import com.oilcollection.repository.CollectionScheduleRepository;
import com.oilcollection.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionScheduleService {

    private final CollectionScheduleRepository scheduleRepository;
    private final CollectionPointRepository pointRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final SystemConfigService systemConfigService;

    @Transactional(readOnly = true)
    public Page<CollectionScheduleDTO> getAllSchedules(Pageable pageable) {
        return scheduleRepository.findAll(pageable).map(CollectionScheduleDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getAllSchedules() {
        return scheduleRepository.findAll().stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CollectionScheduleDTO getScheduleById(UUID id) {
        return scheduleRepository.findById(id)
                .map(CollectionScheduleDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Schedule", "id", id));
    }

    @Transactional(readOnly = true)
    public Page<CollectionScheduleDTO> getSchedulesByCollector(UUID collectorId, Pageable pageable) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return scheduleRepository.findByCollectorAndStatus(collector, null, pageable)
                .map(CollectionScheduleDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getSchedulesByCollectorAndStatus(UUID collectorId, CollectionSchedule.ScheduleStatus status) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return scheduleRepository.findByCollectorAndStatus(collector, status)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getSchedulesByPoint(UUID pointId) {
        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", pointId));
        return scheduleRepository.findByCollectionPointAndStatus(point, null)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getSchedulesByDate(LocalDate date) {
        return scheduleRepository.findByScheduledDate(date)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getUpcomingSchedules(UUID collectorId) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return scheduleRepository.findUpcomingByCollector(collector, LocalDate.now())
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getOverdueSchedules() {
        return scheduleRepository.findOverdueSchedules(
                LocalDate.now(), CollectionSchedule.ScheduleStatus.PENDING)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getSchedulesByCollectorAndDateRange(
            UUID collectorId, LocalDate start, LocalDate end) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return scheduleRepository.findByCollectorAndScheduledDateBetween(collector, start, end)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional
    public CollectionScheduleDTO createSchedule(CollectionScheduleDTO request, User createdBy, String ipAddress, String userAgent) {
        CollectionPoint point = pointRepository.findById(request.getCollectionPointId())
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", request.getCollectionPointId()));

        User collector = userRepository.findById(request.getCollectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", request.getCollectorId()));

        if (collector.getRole() != User.UserRole.COLLECTOR) {
            throw new BusinessException("User must have COLLECTOR role", "INVALID_COLLECTOR_ROLE");
        }

        if (point.getCollector() != null && !point.getCollector().getId().equals(collector.getId())) {
            throw new BusinessException("This collection point is assigned to a different collector", "POINT_ASSIGNED_TO_OTHER");
        }

        // Validate sale type and price
        if (request.getSaleType() == null) {
            request.setSaleType(CollectionSchedule.SaleType.SALE);
        }

        if (request.getSaleType() == CollectionSchedule.SaleType.SALE && request.getBasePricePerLiter() == null) {
            BigDecimal defaultPrice = systemConfigService.getBaseOilPricePerLiter();
            request.setBasePricePerLiter(defaultPrice);
        }

        // Calculate estimated value
        BigDecimal estimatedValue = BigDecimal.ZERO;
        if (request.getEstimatedVolumeLiters() != null && request.getBasePricePerLiter() != null) {
            estimatedValue = request.getBasePricePerLiter().multiply(BigDecimal.valueOf(request.getEstimatedVolumeLiters()));
        }

        CollectionSchedule schedule = new CollectionSchedule();
        schedule.setCollectionPoint(point);
        schedule.setCollector(collector);
        schedule.setScheduledDate(request.getScheduledDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setStatus(CollectionSchedule.ScheduleStatus.PENDING);
        schedule.setEstimatedVolumeLiters(request.getEstimatedVolumeLiters());
        schedule.setNotes(request.getNotes());

        // US02, US06 - Sale/Exchange type and time window
        schedule.setSaleType(request.getSaleType());
        schedule.setTimeWindow(request.getTimeWindow());
        schedule.setBasePricePerLiter(request.getBasePricePerLiter());
        schedule.setCalculatedValue(estimatedValue);

        CollectionSchedule savedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logCreate("COLLECTION_SCHEDULE", savedSchedule.getId(), createdBy,
                toJson(savedSchedule), ipAddress, userAgent);

        // Notify collector
        notificationService.notifyScheduleCreated(collector.getId(), savedSchedule.getId(), point.getName());

        log.info("Schedule created for point {} on {} by {}", point.getName(), request.getScheduledDate(), createdBy.getEmail());
        return CollectionScheduleDTO.fromEntity(savedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO updateSchedule(UUID id, CollectionScheduleDTO request, User updatedBy, String ipAddress, String userAgent) {
        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Schedule", "id", id));

        if (schedule.getStatus() == CollectionSchedule.ScheduleStatus.COMPLETED ||
            schedule.getStatus() == CollectionSchedule.ScheduleStatus.CANCELLED) {
            throw new BusinessException("Cannot update completed or cancelled schedule", "SCHEDULE_IMMUTABLE");
        }

        String oldValues = toJson(schedule);

        if (request.getScheduledDate() != null) {
            schedule.setScheduledDate(request.getScheduledDate());
        }
        if (request.getStartTime() != null) {
            schedule.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            schedule.setEndTime(request.getEndTime());
        }
        if (request.getEstimatedVolumeLiters() != null) {
            schedule.setEstimatedVolumeLiters(request.getEstimatedVolumeLiters());
        }
        if (request.getNotes() != null) {
            schedule.setNotes(request.getNotes());
        }
        // US02, US06 - Sale/Exchange type and price
        if (request.getSaleType() != null) {
            schedule.setSaleType(request.getSaleType());
        }
        if (request.getTimeWindow() != null) {
            schedule.setTimeWindow(request.getTimeWindow());
        }
        if (request.getBasePricePerLiter() != null) {
            schedule.setBasePricePerLiter(request.getBasePricePerLiter());
            // Recalculate value
            if (schedule.getEstimatedVolumeLiters() != null) {
                schedule.setCalculatedValue(request.getBasePricePerLiter()
                        .multiply(BigDecimal.valueOf(schedule.getEstimatedVolumeLiters())));
            }
        }

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logUpdate("COLLECTION_SCHEDULE", updatedSchedule.getId(), updatedBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        log.info("Schedule updated: {} by {}", updatedSchedule.getId(), updatedBy.getEmail());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO assignSchedule(UUID id, UUID collectorId, User assignedBy, String ipAddress, String userAgent) {
        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Schedule", "id", id));

        if (schedule.getStatus() != CollectionSchedule.ScheduleStatus.PENDING) {
            throw new BusinessException("Only pending schedules can be assigned", "INVALID_STATUS_TRANSITION");
        }

        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));

        if (collector.getRole() != User.UserRole.COLLECTOR) {
            throw new BusinessException("User must have COLLECTOR role", "INVALID_COLLECTOR_ROLE");
        }

        String oldValues = toJson(schedule);

        schedule.setCollector(collector);
        schedule.setStatus(CollectionSchedule.ScheduleStatus.ASSIGNED);
        schedule.setAssignedAt(LocalDateTime.now());

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), assignedBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        // Notify collector
        notificationService.notifyScheduleAssigned(collector.getId(), updatedSchedule.getId(), collector.getName());

        log.info("Schedule assigned to collector {}: {}", collector.getName(), updatedSchedule.getId());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO confirmSchedule(UUID id, User confirmedBy, String ipAddress, String userAgent) {
        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Schedule", "id", id));

        // Collector can only confirm their own pending schedules
        if (!schedule.getCollector().getId().equals(confirmedBy.getId())) {
            throw new BusinessException("You can only confirm schedules assigned to you", "UNAUTHORIZED_ACTION");
        }

        if (schedule.getStatus() != CollectionSchedule.ScheduleStatus.PENDING) {
            throw new BusinessException("Only pending schedules can be confirmed", "INVALID_STATUS_TRANSITION");
        }

        String oldValues = toJson(schedule);

        schedule.setStatus(CollectionSchedule.ScheduleStatus.ASSIGNED);
        schedule.setAssignedAt(LocalDateTime.now());

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), confirmedBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        // Notify establishment (find user by contact email)
        if (schedule.getCollectionPoint().getContactEmail() != null) {
            userRepository.findByEmail(schedule.getCollectionPoint().getContactEmail())
                    .ifPresent(establishmentUser ->
                        notificationService.notifyScheduleAssigned(establishmentUser.getId(), updatedSchedule.getId(), confirmedBy.getName())
                    );
        }

        log.info("Schedule confirmed by collector {}: {}", confirmedBy.getName(), updatedSchedule.getId());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO startSchedule(UUID id, User startedBy, String ipAddress, String userAgent) {
        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Schedule", "id", id));

        if (schedule.getStatus() != CollectionSchedule.ScheduleStatus.ASSIGNED &&
            schedule.getStatus() != CollectionSchedule.ScheduleStatus.PENDING) {
            throw new BusinessException("Schedule must be assigned or pending to start", "INVALID_STATUS_TRANSITION");
        }

        String oldValues = toJson(schedule);

        schedule.setStatus(CollectionSchedule.ScheduleStatus.EN_ROUTE);
        schedule.setEnRouteAt(LocalDateTime.now());

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), startedBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        // Notify establishment (find user by contact email)
        if (schedule.getCollectionPoint().getContactEmail() != null) {
            userRepository.findByEmail(schedule.getCollectionPoint().getContactEmail())
                    .ifPresent(establishmentUser ->
                        notificationService.notifyScheduleEnRoute(establishmentUser.getId(), updatedSchedule.getId())
                    );
        }

        log.info("Schedule started (en route): {}", updatedSchedule.getId());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO arriveAtPoint(UUID id, User arrivedBy, String ipAddress, String userAgent) {
        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Schedule", "id", id));

        if (schedule.getStatus() != CollectionSchedule.ScheduleStatus.EN_ROUTE) {
            throw new BusinessException("Schedule must be en route to arrive at point", "INVALID_STATUS_TRANSITION");
        }

        String oldValues = toJson(schedule);

        schedule.setStatus(CollectionSchedule.ScheduleStatus.IN_PROGRESS);

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), arrivedBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        log.info("Collector arrived at point: {}", updatedSchedule.getId());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO completeSchedule(UUID id, User completedBy, String ipAddress, String userAgent) {
        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Schedule", "id", id));

        if (schedule.getStatus() != CollectionSchedule.ScheduleStatus.IN_PROGRESS) {
            throw new BusinessException("Schedule must be in progress to complete", "INVALID_STATUS_TRANSITION");
        }

        String oldValues = toJson(schedule);

        schedule.setStatus(CollectionSchedule.ScheduleStatus.COMPLETED);
        schedule.setCompletedAt(LocalDateTime.now());

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), completedBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        // Notify establishment and collector
        notificationService.notifyScheduleCompleted(schedule.getCollector().getId(), updatedSchedule.getId());
        if (schedule.getCollectionPoint().getContactEmail() != null) {
            userRepository.findByEmail(schedule.getCollectionPoint().getContactEmail())
                    .ifPresent(establishmentUser ->
                        notificationService.notifyScheduleCompleted(establishmentUser.getId(), updatedSchedule.getId())
                    );
        }

        // Check if point is now almost full and trigger alert
        checkAndTriggerAlmostFullAlert(schedule.getCollectionPoint());

        log.info("Schedule completed: {}", updatedSchedule.getId());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO cancelSchedule(UUID id, String reason, User cancelledBy, String ipAddress, String userAgent) {
        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Schedule", "id", id));

        if (schedule.getStatus() == CollectionSchedule.ScheduleStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel completed schedule", "INVALID_STATUS_TRANSITION");
        }

        // US03 - Validate cancellation at least 2 hours before scheduled time
        int hoursBefore = systemConfigService.getCancellationHoursBefore();
        LocalDateTime scheduledDateTime = schedule.getScheduledDate().atTime(schedule.getStartTime());
        LocalDateTime minCancellationTime = scheduledDateTime.minusHours(hoursBefore);

        if (LocalDateTime.now().isAfter(minCancellationTime)) {
            throw new BusinessException(
                    "Cannot cancel schedule less than " + hoursBefore + " hours before scheduled time",
                    "CANCELLATION_TOO_LATE");
        }

        String oldValues = toJson(schedule);

        schedule.setStatus(CollectionSchedule.ScheduleStatus.CANCELLED);
        schedule.setCancelledAt(LocalDateTime.now());
        schedule.setCancellationReason(reason);

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), cancelledBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        // Notify collector
        notificationService.notifyScheduleCancelled(schedule.getCollector().getId(), updatedSchedule.getId(), reason);

        log.info("Schedule cancelled: {} - Reason: {}", updatedSchedule.getId(), reason);
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO rescheduleSchedule(UUID id, LocalDate newDate, LocalTime newStartTime, User rescheduledBy, String ipAddress, String userAgent) {
        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Schedule", "id", id));

        if (schedule.getStatus() == CollectionSchedule.ScheduleStatus.COMPLETED ||
            schedule.getStatus() == CollectionSchedule.ScheduleStatus.CANCELLED) {
            throw new BusinessException("Cannot reschedule completed or cancelled schedule", "INVALID_STATUS_TRANSITION");
        }

        // US03 - Validate reschedule at least 2 hours before scheduled time
        int hoursBefore = systemConfigService.getCancellationHoursBefore();
        LocalDateTime scheduledDateTime = schedule.getScheduledDate().atTime(schedule.getStartTime());
        LocalDateTime minChangeTime = scheduledDateTime.minusHours(hoursBefore);

        if (LocalDateTime.now().isAfter(minChangeTime)) {
            throw new BusinessException(
                    "Cannot reschedule less than " + hoursBefore + " hours before scheduled time",
                    "RESCHEDULE_TOO_LATE");
        }

        String oldValues = toJson(schedule);

        schedule.setStatus(CollectionSchedule.ScheduleStatus.RESCHEDULED);
        schedule.setScheduledDate(newDate);
        if (newStartTime != null) {
            schedule.setStartTime(newStartTime);
        }

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), rescheduledBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        // Notify collector
        notificationService.notifyScheduleCreated(schedule.getCollector().getId(), updatedSchedule.getId(),
                schedule.getCollectionPoint().getName());

        log.info("Schedule rescheduled: {} to {}", updatedSchedule.getId(), newDate);
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public void deleteSchedule(UUID id, User deletedBy, String ipAddress, String userAgent) {
        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Schedule", "id", id));

        if (schedule.getStatus() == CollectionSchedule.ScheduleStatus.COMPLETED) {
            throw new BusinessException("Cannot delete completed schedule", "SCHEDULE_IMMUTABLE");
        }

        String oldValues = toJson(schedule);

        scheduleRepository.delete(schedule);

        // Log audit
        auditLogService.logDelete("COLLECTION_SCHEDULE", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("Schedule deleted: {} by {}", id, deletedBy.getEmail());
    }

    // US11 - Check and trigger almost full alert for collection point
    private void checkAndTriggerAlmostFullAlert(CollectionPoint point) {
        if (point.getCapacityLiters() != null && point.getCapacityLiters() > 0) {
            double fillPercentage = point.getFillPercentage();
            if (fillPercentage >= 80 && !point.getAlertAlmostFull()) {
                point.setAlertAlmostFull(true);
                point.setAlertTriggeredAt(LocalDateTime.now());
                pointRepository.save(point);

                // Notify collector and establishment
                if (point.getCollector() != null) {
                    notificationService.notifyAlmostFull(point.getCollector().getId(), point.getId(), point.getName());
                }
                if (point.getContactEmail() != null) {
                    notificationService.notifyAlmostFull(point.getId(), point.getId(), point.getName());
                }

                log.warn("Almost full alert triggered for point: {} ({}%)", point.getName(), fillPercentage);
            } else if (fillPercentage < 70 && point.getAlertAlmostFull()) {
                // Reset alert if volume drops below 70%
                point.setAlertAlmostFull(false);
                point.setAlertTriggeredAt(null);
                pointRepository.save(point);
            }
        }
    }

    private String toJson(CollectionSchedule schedule) {
        return String.format(
                "{\"id\":\"%s\",\"status\":\"%s\",\"scheduledDate\":\"%s\",\"collector\":\"%s\",\"point\":\"%s\"}",
                schedule.getId(),
                schedule.getStatus(),
                schedule.getScheduledDate(),
                schedule.getCollector() != null ? schedule.getCollector().getName() : "null",
                schedule.getCollectionPoint() != null ? schedule.getCollectionPoint().getName() : "null"
        );
    }
}