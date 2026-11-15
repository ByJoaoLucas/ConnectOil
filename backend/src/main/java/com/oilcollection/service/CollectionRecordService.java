package com.oilcollection.service;

import com.oilcollection.dto.CollectionRecordDTO;
import com.oilcollection.dto.CollectorPerformanceDTO;
import com.oilcollection.dto.DailyVolumeDTO;
import com.oilcollection.dto.OilTypeVolumeDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.CollectionRecord;
import com.oilcollection.model.CollectionSchedule;
import com.oilcollection.model.User;
import com.oilcollection.repository.CollectionPointRepository;
import com.oilcollection.repository.CollectionRecordRepository;
import com.oilcollection.repository.CollectionScheduleRepository;
import com.oilcollection.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionRecordService {

    private final CollectionRecordRepository recordRepository;
    private final CollectionPointRepository pointRepository;
    private final CollectionScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final SystemConfigService systemConfigService;

    private static final String CONFIRMATION_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CONFIRMATION_CODE_LENGTH = 8;
    private static final String CERTIFICATE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CERTIFICATE_CODE_LENGTH = 12;
    private final Random random = new Random();

    @Transactional(readOnly = true)
    public Page<CollectionRecordDTO> getAllRecords(Pageable pageable) {
        return recordRepository.findAll(pageable).map(CollectionRecordDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionRecordDTO> getAllRecords() {
        return recordRepository.findAll().stream().map(CollectionRecordDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CollectionRecordDTO getRecordById(UUID id) {
        return recordRepository.findById(id)
                .map(CollectionRecordDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Record", "id", id));
    }

    @Transactional(readOnly = true)
    public Page<CollectionRecordDTO> getRecordsByPoint(UUID pointId, Pageable pageable) {
        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", pointId));
        return recordRepository.findByCollectionPoint(point, pageable).map(CollectionRecordDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CollectionRecordDTO> getRecordsByCollector(UUID collectorId, Pageable pageable) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return recordRepository.findByCollector(collector, pageable).map(CollectionRecordDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionRecordDTO> getRecordsByCollectorAndDateRange(
            UUID collectorId, LocalDateTime start, LocalDateTime end) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return recordRepository.findByCollectorAndDateRange(collector, start, end)
                .stream().map(CollectionRecordDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionRecordDTO> getRecordsByDateRange(LocalDateTime start, LocalDateTime end) {
        return recordRepository.findByCollectionDateBetween(start, end)
                .stream().map(CollectionRecordDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionRecordDTO> getPendingVerificationRecords() {
        return recordRepository.findByVerifiedByManagerFalse()
                .stream().map(CollectionRecordDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionRecordDTO> getPendingVerificationByCollector(UUID collectorId) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return recordRepository.findPendingVerificationByCollector(collector)
                .stream().map(CollectionRecordDTO::fromEntity).toList();
    }

    @Transactional
    public CollectionRecordDTO createRecord(CollectionRecordDTO request, User createdBy, String ipAddress, String userAgent) {
        CollectionPoint point = pointRepository.findById(request.getCollectionPointId())
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", request.getCollectionPointId()));

        User collector = userRepository.findById(request.getCollectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", request.getCollectorId()));

        if (collector.getRole() != User.UserRole.COLLECTOR) {
            throw new BusinessException("User must have COLLECTOR role", "INVALID_COLLECTOR_ROLE");
        }

        CollectionSchedule schedule = null;
        if (request.getScheduleId() != null) {
            schedule = scheduleRepository.findById(request.getScheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", request.getScheduleId()));

            if (!schedule.getCollector().getId().equals(collector.getId())) {
                throw new BusinessException("Schedule does not belong to this collector", "SCHEDULE_MISMATCH");
            }
            if (!schedule.getCollectionPoint().getId().equals(point.getId())) {
                throw new BusinessException("Schedule does not belong to this collection point", "SCHEDULE_MISMATCH");
            }
        }

        // US05, US14 - Generate confirmation code
        String confirmationCode = generateConfirmationCode();

        // US05 - Discrepancy detection
        boolean discrepancyFlag = false;
        String discrepancyDetails = null;
        if (schedule != null && schedule.getEstimatedVolumeLiters() != null && request.getVolumeCollectedLiters() != null) {
            int estimated = schedule.getEstimatedVolumeLiters();
            int actual = request.getVolumeCollectedLiters().intValue();
            int threshold = systemConfigService.getDiscrepancyThresholdPercentage();
            double diffPercentage = Math.abs((actual - estimated) * 100.0 / estimated);

            if (diffPercentage > threshold) {
                discrepancyFlag = true;
                discrepancyDetails = String.format("Volume discrepancy: estimated %dL, actual %dL (%.1f%% difference)",
                        estimated, actual, diffPercentage);
            }
        }

        CollectionRecord record = new CollectionRecord();
        record.setCollectionPoint(point);
        record.setCollector(collector);
        record.setSchedule(schedule);
        record.setCollectionDate(request.getCollectionDate());
        record.setVolumeCollectedLiters(request.getVolumeCollectedLiters());
        record.setOilType(request.getOilType());
        record.setContainerCount(request.getContainerCount());
        record.setContainerType(request.getContainerType());
        record.setQualityRating(request.getQualityRating());
        record.setTemperatureCelsius(request.getTemperatureCelsius());
        record.setPhLevel(request.getPhLevel());
        record.setContaminationPercentage(request.getContaminationPercentage());
        record.setPhotos(request.getPhotos());
        record.setNotes(request.getNotes());
        record.setGpsLatitude(request.getGpsLatitude());
        record.setGpsLongitude(request.getGpsLongitude());

        // US05, US14 - Set confirmation code and discrepancy flag
        record.setConfirmationCode(confirmationCode);
        record.setDiscrepancyFlag(discrepancyFlag);
        record.setDiscrepancyReason(discrepancyDetails);

        CollectionRecord savedRecord = recordRepository.save(record);

        // Update collection point volume
        point.setCurrentVolumeLiters(point.getCurrentVolumeLiters() - request.getVolumeCollectedLiters().intValue());
        if (point.getCurrentVolumeLiters() < 0) {
            point.setCurrentVolumeLiters(0);
        }
        if (point.isFull()) {
            point.setStatus(CollectionPoint.PointStatus.FULL);
        } else if (point.getStatus() == CollectionPoint.PointStatus.FULL) {
            point.setStatus(CollectionPoint.PointStatus.ACTIVE);
        }
        pointRepository.save(point);

        // Update schedule if linked
        if (schedule != null && (schedule.getStatus() == CollectionSchedule.ScheduleStatus.IN_PROGRESS || schedule.getStatus() == CollectionSchedule.ScheduleStatus.CONFIRMED)) {
            schedule.setStatus(CollectionSchedule.ScheduleStatus.COMPLETED);
            schedule.setCompletedAt(LocalDateTime.now());
            scheduleRepository.save(schedule);
        }

        // Log audit
        auditLogService.logCreate("COLLECTION_RECORD", savedRecord.getId(), createdBy,
                toJson(savedRecord), ipAddress, userAgent);

        // US05 - Notify about discrepancy if detected
        if (discrepancyFlag) {
            notificationService.notifyDiscrepancy(point.getCollector() != null ? point.getCollector().getId() : collector.getId(),
                    savedRecord.getId(), discrepancyDetails);
        }

        // US03 - Notify schedule completed
        if (schedule != null) {
            notificationService.notifyScheduleCompleted(collector.getId(), schedule.getId());
            notificationService.notifyCertificateReady(collector.getId(), savedRecord.getId());
        }

        log.info("Collection record created: {} liters from {} (confirmation: {})",
                request.getVolumeCollectedLiters(), point.getName(), confirmationCode);
        return CollectionRecordDTO.fromEntity(savedRecord);
    }

    @Transactional
    public CollectionRecordDTO updateRecord(UUID id, CollectionRecordDTO request, User updatedBy, String ipAddress, String userAgent) {
        CollectionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Record", "id", id));

        if (record.getVerifiedByManager()) {
            throw new BusinessException("Cannot update verified record", "RECORD_VERIFIED");
        }

        String oldValues = toJson(record);

        // Calculate volume difference for point update
        BigDecimal oldVolume = record.getVolumeCollectedLiters();
        BigDecimal newVolume = request.getVolumeCollectedLiters();
        int volumeDiff = newVolume.subtract(oldVolume).intValue();

        record.setCollectionDate(request.getCollectionDate());
        record.setVolumeCollectedLiters(newVolume);
        record.setOilType(request.getOilType());
        record.setContainerCount(request.getContainerCount());
        record.setContainerType(request.getContainerType());
        record.setQualityRating(request.getQualityRating());
        record.setTemperatureCelsius(request.getTemperatureCelsius());
        record.setPhLevel(request.getPhLevel());
        record.setContaminationPercentage(request.getContaminationPercentage());
        record.setPhotos(request.getPhotos());
        record.setNotes(request.getNotes());
        record.setGpsLatitude(request.getGpsLatitude());
        record.setGpsLongitude(request.getGpsLongitude());

        // Re-check discrepancy on update
        if (record.getSchedule() != null && record.getSchedule().getEstimatedVolumeLiters() != null) {
            int estimated = record.getSchedule().getEstimatedVolumeLiters();
            int actual = newVolume.intValue();
            int threshold = systemConfigService.getDiscrepancyThresholdPercentage();
            double diffPercentage = Math.abs((actual - estimated) * 100.0 / estimated);

            boolean newDiscrepancyFlag = diffPercentage > threshold;
            String newDiscrepancyDetails = newDiscrepancyFlag ?
                    String.format("Volume discrepancy: estimated %dL, actual %dL (%.1f%% difference)",
                            estimated, actual, diffPercentage) : null;

            record.setDiscrepancyFlag(newDiscrepancyFlag);
            record.setDiscrepancyReason(newDiscrepancyDetails);
        }

        CollectionRecord updatedRecord = recordRepository.save(record);

        // Update collection point volume
        CollectionPoint point = record.getCollectionPoint();
        point.setCurrentVolumeLiters(point.getCurrentVolumeLiters() - volumeDiff);
        if (point.getCurrentVolumeLiters() < 0) {
            point.setCurrentVolumeLiters(0);
        }
        pointRepository.save(point);

        // Log audit
        auditLogService.logUpdate("COLLECTION_RECORD", updatedRecord.getId(), updatedBy,
                oldValues, toJson(updatedRecord), ipAddress, userAgent);

        log.info("Collection record updated: {}", updatedRecord.getId());
        return CollectionRecordDTO.fromEntity(updatedRecord);
    }

    @Transactional
    public CollectionRecordDTO verifyRecord(UUID id, UUID managerId, User verifiedBy, String ipAddress, String userAgent) {
        CollectionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Record", "id", id));

        if (record.getVerifiedByManager()) {
            throw new BusinessException("Record already verified", "ALREADY_VERIFIED");
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", managerId));

        String oldValues = toJson(record);

        // US07, US14 - Generate certificate code on verification
        String certificateCode = generateCertificateCode();

        record.setVerifiedByManager(true);
        record.setVerifiedAt(LocalDateTime.now());
        record.setVerifiedBy(manager);
        record.setCertificateCode(certificateCode);

        CollectionRecord verifiedRecord = recordRepository.save(record);

        // Log audit
        auditLogService.logUpdate("COLLECTION_RECORD", verifiedRecord.getId(), verifiedBy,
                oldValues, toJson(verifiedRecord), ipAddress, userAgent);

        // US07 - Notify certificate ready
        notificationService.notifyCertificateReady(record.getCollector().getId(), verifiedRecord.getId());

        log.info("Collection record verified: {} by {} (certificate: {})", id, manager.getName(), certificateCode);
        return CollectionRecordDTO.fromEntity(verifiedRecord);
    }

    // US05 - Establishment confirms the collection
    @Transactional
    public CollectionRecordDTO confirmCollectionByEstablishment(UUID id, String confirmationCode, User establishmentUser, String ipAddress, String userAgent) {
        CollectionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Record", "id", id));

        if (record.getEstablishmentConfirmed()) {
            throw new BusinessException("Collection already confirmed by establishment", "ALREADY_CONFIRMED");
        }

        if (!confirmationCode.equals(record.getConfirmationCode())) {
            throw new BusinessException("Invalid confirmation code", "INVALID_CONFIRMATION_CODE");
        }

        String oldValues = toJson(record);

        record.setEstablishmentConfirmed(true);
        record.setEstablishmentConfirmedAt(LocalDateTime.now());
        record.setEstablishmentConfirmedBy(establishmentUser);

        CollectionRecord confirmedRecord = recordRepository.save(record);

        // Log audit
        auditLogService.logUpdate("COLLECTION_RECORD", confirmedRecord.getId(), establishmentUser,
                oldValues, toJson(confirmedRecord), ipAddress, userAgent);

        log.info("Collection confirmed by establishment: {} (code: {})", id, confirmationCode);
        return CollectionRecordDTO.fromEntity(confirmedRecord);
    }

    @Transactional
    public void deleteRecord(UUID id, User deletedBy, String ipAddress, String userAgent) {
        CollectionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Record", "id", id));

        if (record.getVerifiedByManager()) {
            throw new BusinessException("Cannot delete verified record", "RECORD_VERIFIED");
        }

        String oldValues = toJson(record);

        // Restore volume to collection point
        CollectionPoint point = record.getCollectionPoint();
        point.setCurrentVolumeLiters(point.getCurrentVolumeLiters() + record.getVolumeCollectedLiters().intValue());
        pointRepository.save(point);

        recordRepository.delete(record);

        // Log audit
        auditLogService.logDelete("COLLECTION_RECORD", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("Collection record deleted: {} by {}", id, deletedBy.getEmail());
    }

    // Reports
    @Transactional(readOnly = true)
    public BigDecimal getTotalVolumeByCollector(UUID collectorId, LocalDateTime start, LocalDateTime end) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return recordRepository.getTotalVolumeByCollectorAndPeriod(collector, start, end);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalVolumeByPoint(UUID pointId, LocalDateTime start, LocalDateTime end) {
        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", pointId));
        return recordRepository.getTotalVolumeByPointAndPeriod(point, start, end);
    }

    @Transactional(readOnly = true)
    public long getCollectionCountByCollector(UUID collectorId, LocalDateTime start, LocalDateTime end) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return recordRepository.countByCollectorAndPeriod(collector, start, end);
    }

    @Transactional(readOnly = true)
    public Double getAverageQualityByCollector(UUID collectorId, LocalDateTime start, LocalDateTime end) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return recordRepository.getAverageQualityByCollectorAndPeriod(collector, start, end);
    }

    @Transactional(readOnly = true)
    public List<DailyVolumeDTO> getDailyVolumeReport(LocalDateTime start, LocalDateTime end) {
        return recordRepository.getDailyVolumeReport(start, end);
    }

    @Transactional(readOnly = true)
    public List<CollectorPerformanceDTO> getCollectorPerformanceReport(LocalDateTime start, LocalDateTime end) {
        return recordRepository.getCollectorPerformanceReport(start, end);
    }

    @Transactional(readOnly = true)
    public List<OilTypeVolumeDTO> getOilTypeVolumeReport(LocalDateTime start, LocalDateTime end) {
        return recordRepository.getOilTypeVolumeReport(start, end);
    }

    // US07, US14 - Generate PDF certificate data
    @Transactional(readOnly = true)
    public CollectionRecordDTO generateCertificate(UUID id) {
        CollectionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Record", "id", id));

        if (!record.getVerifiedByManager()) {
            throw new BusinessException("Record must be verified before generating certificate", "NOT_VERIFIED");
        }

        return CollectionRecordDTO.fromEntity(record);
    }

    private String generateConfirmationCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CONFIRMATION_CODE_LENGTH; i++) {
            sb.append(CONFIRMATION_CODE_CHARS.charAt(random.nextInt(CONFIRMATION_CODE_CHARS.length())));
        }
        // Ensure uniqueness
        if (recordRepository.existsByConfirmationCode(sb.toString())) {
            return generateConfirmationCode();
        }
        return sb.toString();
    }

    private String generateCertificateCode() {
        StringBuilder sb = new StringBuilder("CERT-");
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        sb.append(datePart).append("-");
        for (int i = 0; i < CERTIFICATE_CODE_LENGTH; i++) {
            sb.append(CERTIFICATE_CODE_CHARS.charAt(random.nextInt(CERTIFICATE_CODE_CHARS.length())));
        }
        // Ensure uniqueness
        if (recordRepository.existsByCertificateCode(sb.toString())) {
            return generateCertificateCode();
        }
        return sb.toString();
    }

    private String toJson(CollectionRecord record) {
        return String.format(
                "{\"id\":\"%s\",\"volume\":%s,\"oilType\":\"%s\",\"point\":\"%s\",\"collector\":\"%s\",\"verified\":%b,\"confirmationCode\":\"%s\",\"certificateCode\":\"%s\",\"discrepancy\":%b}",
                record.getId(),
                record.getVolumeCollectedLiters(),
                record.getOilType(),
                record.getCollectionPoint() != null ? record.getCollectionPoint().getName() : "null",
                record.getCollector() != null ? record.getCollector().getName() : "null",
                record.getVerifiedByManager(),
                record.getConfirmationCode(),
                record.getCertificateCode(),
                record.getDiscrepancyFlag()
        );
    }
}