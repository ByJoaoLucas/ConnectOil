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

    private static final String LOG_PREFIX = "[CollectionRecordService]";

    @Transactional(readOnly = true)
    public Page<CollectionRecordDTO> getAllRecords(Pageable pageable) {
        log.debug("{} Buscando todos os registros de coleta com paginação: {}", LOG_PREFIX, pageable);
        return recordRepository.findAll(pageable).map(CollectionRecordDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionRecordDTO> getAllRecords() {
        log.debug("{} Buscando todos os registros de coleta como lista", LOG_PREFIX);
        return recordRepository.findAll().stream().map(CollectionRecordDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CollectionRecordDTO getRecordById(UUID id) {
        log.debug("{} Buscando registro de coleta por ID: {}", LOG_PREFIX, id);
        return recordRepository.findById(id)
                .map(CollectionRecordDTO::fromEntity)
                .orElseThrow(() -> {
                    log.warn("{} Registro de coleta não encontrado: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Record", "id", id);
                });
    }

    @Transactional(readOnly = true)
    public Page<CollectionRecordDTO> getRecordsByPoint(UUID pointId, Pageable pageable) {
        log.debug("{} Buscando registros por ponto de coleta ID: {} com paginação: {}", LOG_PREFIX, pointId, pageable);
        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado: {}", LOG_PREFIX, pointId);
                    return new ResourceNotFoundException("Collection Point", "id", pointId);
                });
        return recordRepository.findByCollectionPoint(point, pageable).map(CollectionRecordDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CollectionRecordDTO> getRecordsByCollector(UUID collectorId, Pageable pageable) {
        log.debug("{} Buscando registros por coletor ID: {} com paginação: {}", LOG_PREFIX, collectorId, pageable);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return recordRepository.findByCollector(collector, pageable).map(CollectionRecordDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionRecordDTO> getRecordsByCollectorAndDateRange(
            UUID collectorId, LocalDateTime start, LocalDateTime end) {
        log.debug("{} Buscando registros por coletor ID: {} entre {} e {}", LOG_PREFIX, collectorId, start, end);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return recordRepository.findByCollectorAndDateRange(collector, start, end)
                .stream().map(CollectionRecordDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionRecordDTO> getRecordsByDateRange(LocalDateTime start, LocalDateTime end) {
        log.debug("{} Buscando registros entre {} e {}", LOG_PREFIX, start, end);
        return recordRepository.findByCollectionDateBetween(start, end)
                .stream().map(CollectionRecordDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionRecordDTO> getPendingVerificationRecords() {
        log.debug("{} Buscando registros pendentes de verificação", LOG_PREFIX);
        return recordRepository.findByVerifiedByManagerFalse()
                .stream().map(CollectionRecordDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionRecordDTO> getPendingVerificationByCollector(UUID collectorId) {
        log.debug("{} Buscando registros pendentes de verificação por coletor ID: {}", LOG_PREFIX, collectorId);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return recordRepository.findPendingVerificationByCollector(collector)
                .stream().map(CollectionRecordDTO::fromEntity).toList();
    }

    @Transactional
    public CollectionRecordDTO createRecord(CollectionRecordDTO request, User createdBy, String ipAddress, String userAgent) {
        log.info("{} Criando registro de coleta para ponto ID: {} por coletor ID: {} (criado por: {})",
                LOG_PREFIX, request.getCollectionPointId(), request.getCollectorId(), createdBy.getEmail());

        CollectionPoint point = pointRepository.findById(request.getCollectionPointId())
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado: {}", LOG_PREFIX, request.getCollectionPointId());
                    return new ResourceNotFoundException("Collection Point", "id", request.getCollectionPointId());
                });

        User collector = userRepository.findById(request.getCollectorId())
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, request.getCollectorId());
                    return new ResourceNotFoundException("Collector", "id", request.getCollectorId());
                });

        if (collector.getRole() != User.UserRole.COLLECTOR) {
            log.warn("{} Usuário não é coletor: {} (role: {})", LOG_PREFIX, collector.getEmail(), collector.getRole());
            throw new BusinessException("Usuário deve ter papel de COLETOR", "INVALID_COLLECTOR_ROLE");
        }

        CollectionSchedule schedule = null;
        if (request.getScheduleId() != null) {
            schedule = scheduleRepository.findById(request.getScheduleId())
                    .orElseThrow(() -> {
                        log.warn("{} Agendamento não encontrado: {}", LOG_PREFIX, request.getScheduleId());
                        return new ResourceNotFoundException("Schedule", "id", request.getScheduleId());
                    });

            if (!schedule.getCollector().getId().equals(collector.getId())) {
                log.warn("{} Agendamento {} não pertence ao coletor {}", LOG_PREFIX, schedule.getId(), collector.getId());
                throw new BusinessException("Agendamento não pertence a este coletor", "SCHEDULE_MISMATCH");
            }
            if (!schedule.getCollectionPoint().getId().equals(point.getId())) {
                log.warn("{} Agendamento {} não pertence ao ponto de coleta {}", LOG_PREFIX, schedule.getId(), point.getId());
                throw new BusinessException("Agendamento não pertence a este ponto de coleta", "SCHEDULE_MISMATCH");
            }
            log.debug("{} Registro vinculado ao agendamento: {}", LOG_PREFIX, schedule.getId());
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
                discrepancyDetails = String.format("Discrepância de volume: estimado %dL, real %dL (%.1f%% de diferença)",
                        estimated, actual, diffPercentage);
                log.warn("{} Discrepância detectada no registro: estimado={}L, real={}L, diferença={}%", LOG_PREFIX, estimated, actual, diffPercentage);
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
        int newVolume = point.getCurrentVolumeLiters() - request.getVolumeCollectedLiters().intValue();
        point.setCurrentVolumeLiters(Math.max(0, newVolume));
        if (point.isFull()) {
            point.setStatus(CollectionPoint.PointStatus.FULL);
        } else if (point.getStatus() == CollectionPoint.PointStatus.FULL) {
            point.setStatus(CollectionPoint.PointStatus.ACTIVE);
        }
        pointRepository.save(point);
        log.debug("{} Volume do ponto {} atualizado para: {}L", LOG_PREFIX, point.getName(), point.getCurrentVolumeLiters());

        // Update schedule if linked
        if (schedule != null && (schedule.getStatus() == CollectionSchedule.ScheduleStatus.IN_PROGRESS || schedule.getStatus() == CollectionSchedule.ScheduleStatus.CONFIRMED)) {
            schedule.setStatus(CollectionSchedule.ScheduleStatus.COMPLETED);
            schedule.setCompletedAt(LocalDateTime.now());
            scheduleRepository.save(schedule);
            log.debug("{} Agendamento {} marcado como concluído", LOG_PREFIX, schedule.getId());
        }

        // Log audit
        auditLogService.logCreate("COLLECTION_RECORD", savedRecord.getId(), createdBy,
                toJson(savedRecord), ipAddress, userAgent);

        // US05 - Notify about discrepancy if detected
        if (discrepancyFlag) {
            UUID notifyUserId = point.getCollector() != null ? point.getCollector().getId() : collector.getId();
            notificationService.notifyDiscrepancy(notifyUserId, savedRecord.getId(), discrepancyDetails);
            log.debug("{} Notificação de discrepância enviada para usuário ID: {}", LOG_PREFIX, notifyUserId);
        }

        // US03 - Notify schedule completed
        if (schedule != null) {
            notificationService.notifyScheduleCompleted(collector.getId(), schedule.getId());
            notificationService.notifyCertificateReady(collector.getId(), savedRecord.getId());
            log.debug("{} Notificações de conclusão e certificado enviadas para coletor: {}", LOG_PREFIX, collector.getEmail());
        }

        log.info("{} Registro de coleta criado: ID={}, {}L do ponto {} (código confirmação: {})",
                LOG_PREFIX, savedRecord.getId(), request.getVolumeCollectedLiters(), point.getName(), confirmationCode);
        return CollectionRecordDTO.fromEntity(savedRecord);
    }

    @Transactional
    public CollectionRecordDTO updateRecord(UUID id, CollectionRecordDTO request, User updatedBy, String ipAddress, String userAgent) {
        log.info("{} Atualizando registro de coleta ID: {} por usuário: {}", LOG_PREFIX, id, updatedBy.getEmail());

        CollectionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Registro de coleta não encontrado para atualização: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Record", "id", id);
                });

        if (record.getVerifiedByManager()) {
            log.warn("{} Tentativa de atualizar registro verificado: {}", LOG_PREFIX, id);
            throw new BusinessException("Não é possível atualizar registro verificado", "RECORD_VERIFIED");
        }

        String oldValues = toJson(record);
        log.debug("{} Valores anteriores do registro {}: {}", LOG_PREFIX, id, oldValues);

        // Calculate volume difference for point update
        BigDecimal oldVolume = record.getVolumeCollectedLiters();
        BigDecimal newVolume = request.getVolumeCollectedLiters();
        int volumeDiff = newVolume.subtract(oldVolume).intValue();

        log.debug("{} Atualizando registro {}: volume {} -> {}, tipo óleo {} -> {}",
                LOG_PREFIX, id, oldVolume, newVolume, record.getOilType(), request.getOilType());

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
                    String.format("Discrepância de volume: estimado %dL, real %dL (%.1f%% de diferença)",
                            estimated, actual, diffPercentage) : null;

            if (newDiscrepancyFlag != record.getDiscrepancyFlag()) {
                log.warn("{} Status de discrepância alterado no registro {}: {} -> {}", LOG_PREFIX, id, record.getDiscrepancyFlag(), newDiscrepancyFlag);
            }
            record.setDiscrepancyFlag(newDiscrepancyFlag);
            record.setDiscrepancyReason(newDiscrepancyDetails);
        }

        CollectionRecord updatedRecord = recordRepository.save(record);

        // Update collection point volume
        CollectionPoint point = record.getCollectionPoint();
        point.setCurrentVolumeLiters(Math.max(0, point.getCurrentVolumeLiters() - volumeDiff));
        pointRepository.save(point);
        log.debug("{} Volume do ponto {} atualizado para: {}L (diferença: {})", LOG_PREFIX, point.getName(), point.getCurrentVolumeLiters(), volumeDiff);

        // Log audit
        auditLogService.logUpdate("COLLECTION_RECORD", updatedRecord.getId(), updatedBy,
                oldValues, toJson(updatedRecord), ipAddress, userAgent);

        log.info("{} Registro de coleta atualizado: ID={}, por={}", LOG_PREFIX, updatedRecord.getId(), updatedBy.getEmail());
        return CollectionRecordDTO.fromEntity(updatedRecord);
    }

    @Transactional
    public CollectionRecordDTO verifyRecord(UUID id, UUID managerId, User verifiedBy, String ipAddress, String userAgent) {
        log.info("{} Verificando registro de coleta ID: {} por gerente ID: {} (verificado por: {})",
                LOG_PREFIX, id, managerId, verifiedBy.getEmail());

        CollectionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Registro de coleta não encontrado para verificação: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Record", "id", id);
                });

        if (record.getVerifiedByManager()) {
            log.warn("{} Tentativa de verificar registro já verificado: {}", LOG_PREFIX, id);
            throw new BusinessException("Registro já verificado", "ALREADY_VERIFIED");
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> {
                    log.warn("{} Gerente não encontrado: {}", LOG_PREFIX, managerId);
                    return new ResourceNotFoundException("Manager", "id", managerId);
                });

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
        log.debug("{} Notificação de certificado pronto enviada para coletor: {}", LOG_PREFIX, record.getCollector().getEmail());

        log.info("{} Registro de coleta verificado: ID={}, por gerente={}, certificado={}",
                LOG_PREFIX, id, manager.getName(), certificateCode);
        return CollectionRecordDTO.fromEntity(verifiedRecord);
    }

    // US05 - Establishment confirms the collection
    @Transactional
    public CollectionRecordDTO confirmCollectionByEstablishment(UUID id, String confirmationCode, User establishmentUser, String ipAddress, String userAgent) {
        log.info("{} Confirmando coleta por estabelecimento - registro ID: {} por usuário: {}",
                LOG_PREFIX, id, establishmentUser.getEmail());

        CollectionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Registro de coleta não encontrado para confirmação: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Record", "id", id);
                });

        if (record.getEstablishmentConfirmed()) {
            log.warn("{} Tentativa de confirmar coleta já confirmada: {}", LOG_PREFIX, id);
            throw new BusinessException("Coleta já confirmada pelo estabelecimento", "ALREADY_CONFIRMED");
        }

        if (!confirmationCode.equals(record.getConfirmationCode())) {
            log.warn("{} Código de confirmação inválido para registro {}: fornecido={}, esperado={}",
                    LOG_PREFIX, id, confirmationCode, record.getConfirmationCode());
            throw new BusinessException("Código de confirmação inválido", "INVALID_CONFIRMATION_CODE");
        }

        String oldValues = toJson(record);

        record.setEstablishmentConfirmed(true);
        record.setEstablishmentConfirmedAt(LocalDateTime.now());
        record.setEstablishmentConfirmedBy(establishmentUser);

        CollectionRecord confirmedRecord = recordRepository.save(record);

        // Log audit
        auditLogService.logUpdate("COLLECTION_RECORD", confirmedRecord.getId(), establishmentUser,
                oldValues, toJson(confirmedRecord), ipAddress, userAgent);

        log.info("{} Coleta confirmada pelo estabelecimento: ID={}, código={}, por={}",
                LOG_PREFIX, id, confirmationCode, establishmentUser.getEmail());
        return CollectionRecordDTO.fromEntity(confirmedRecord);
    }

    @Transactional
    public void deleteRecord(UUID id, User deletedBy, String ipAddress, String userAgent) {
        log.info("{} Excluindo registro de coleta ID: {} por usuário: {}", LOG_PREFIX, id, deletedBy.getEmail());

        CollectionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Registro de coleta não encontrado para exclusão: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Record", "id", id);
                });

        if (record.getVerifiedByManager()) {
            log.warn("{} Tentativa de excluir registro verificado: {}", LOG_PREFIX, id);
            throw new BusinessException("Não é possível excluir registro verificado", "RECORD_VERIFIED");
        }

        String oldValues = toJson(record);

        // Restore volume to collection point
        CollectionPoint point = record.getCollectionPoint();
        point.setCurrentVolumeLiters(point.getCurrentVolumeLiters() + record.getVolumeCollectedLiters().intValue());
        pointRepository.save(point);
        log.debug("{} Volume do ponto {} restaurado para: {}L", LOG_PREFIX, point.getName(), point.getCurrentVolumeLiters());

        recordRepository.delete(record);

        // Log audit
        auditLogService.logDelete("COLLECTION_RECORD", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("{} Registro de coleta excluído: ID={}, por={}", LOG_PREFIX, id, deletedBy.getEmail());
    }

    // Reports
    @Transactional(readOnly = true)
    public BigDecimal getTotalVolumeByCollector(UUID collectorId, LocalDateTime start, LocalDateTime end) {
        log.debug("{} Buscando volume total por coletor ID: {} entre {} e {}", LOG_PREFIX, collectorId, start, end);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return recordRepository.getTotalVolumeByCollectorAndPeriod(collector, start, end);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalVolumeByPoint(UUID pointId, LocalDateTime start, LocalDateTime end) {
        log.debug("{} Buscando volume total por ponto ID: {} entre {} e {}", LOG_PREFIX, pointId, start, end);
        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado: {}", LOG_PREFIX, pointId);
                    return new ResourceNotFoundException("Collection Point", "id", pointId);
                });
        return recordRepository.getTotalVolumeByPointAndPeriod(point, start, end);
    }

    @Transactional(readOnly = true)
    public long getCollectionCountByCollector(UUID collectorId, LocalDateTime start, LocalDateTime end) {
        log.debug("{} Buscando contagem de coletas por coletor ID: {} entre {} e {}", LOG_PREFIX, collectorId, start, end);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return recordRepository.countByCollectorAndPeriod(collector, start, end);
    }

    @Transactional(readOnly = true)
    public Double getAverageQualityByCollector(UUID collectorId, LocalDateTime start, LocalDateTime end) {
        log.debug("{} Buscando qualidade média por coletor ID: {} entre {} e {}", LOG_PREFIX, collectorId, start, end);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return recordRepository.getAverageQualityByCollectorAndPeriod(collector, start, end);
    }

    @Transactional(readOnly = true)
    public List<DailyVolumeDTO> getDailyVolumeReport(LocalDateTime start, LocalDateTime end) {
        log.debug("{} Gerando relatório de volume diário entre {} e {}", LOG_PREFIX, start, end);
        return recordRepository.getDailyVolumeReport(start, end);
    }

    @Transactional(readOnly = true)
    public List<CollectorPerformanceDTO> getCollectorPerformanceReport(LocalDateTime start, LocalDateTime end) {
        log.debug("{} Gerando relatório de performance de coletores entre {} e {}", LOG_PREFIX, start, end);
        return recordRepository.getCollectorPerformanceReport(start, end);
    }

    @Transactional(readOnly = true)
    public List<OilTypeVolumeDTO> getOilTypeVolumeReport(LocalDateTime start, LocalDateTime end) {
        log.debug("{} Gerando relatório de volume por tipo de óleo entre {} e {}", LOG_PREFIX, start, end);
        return recordRepository.getOilTypeVolumeReport(start, end);
    }

    // US07, US14 - Generate PDF certificate data
    @Transactional(readOnly = true)
    public CollectionRecordDTO generateCertificate(UUID id) {
        log.debug("{} Gerando certificado para registro ID: {}", LOG_PREFIX, id);
        CollectionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Registro de coleta não encontrado para certificado: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Record", "id", id);
                });

        if (!record.getVerifiedByManager()) {
            log.warn("{} Tentativa de gerar certificado para registro não verificado: {}", LOG_PREFIX, id);
            throw new BusinessException("Registro deve ser verificado antes de gerar certificado", "NOT_VERIFIED");
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
            log.debug("{} Código de confirmação duplicado, gerando novo", LOG_PREFIX);
            return generateConfirmationCode();
        }
        log.debug("{} Código de confirmação gerado: {}", LOG_PREFIX, sb.toString());
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
            log.debug("{} Código de certificado duplicado, gerando novo", LOG_PREFIX);
            return generateCertificateCode();
        }
        log.debug("{} Código de certificado gerado: {}", LOG_PREFIX, sb.toString());
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