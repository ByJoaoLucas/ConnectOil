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
import java.time.ZoneId;
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

    private static final String LOG_PREFIX = "[CollectionScheduleService]";

    @Transactional(readOnly = true)
    public Page<CollectionScheduleDTO> getAllSchedules(Pageable pageable) {
        log.debug("{} Buscando todos os agendamentos com paginação: {}", LOG_PREFIX, pageable);
        return scheduleRepository.findAll(pageable).map(CollectionScheduleDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getAllSchedules() {
        log.debug("{} Buscando todos os agendamentos como lista", LOG_PREFIX);
        return scheduleRepository.findAllByOrderByCreatedAtDesc().stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CollectionScheduleDTO getScheduleById(UUID id) {
        log.debug("{} Buscando agendamento por ID: {}", LOG_PREFIX, id);
        return scheduleRepository.findById(id)
                .map(CollectionScheduleDTO::fromEntity)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Schedule", "id", id);
                });
    }

    @Transactional(readOnly = true)
    public Page<CollectionScheduleDTO> getSchedulesByCollector(UUID collectorId, Pageable pageable) {
        log.debug("{} Buscando agendamentos por coletor ID: {} com paginação: {}", LOG_PREFIX, collectorId, pageable);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return scheduleRepository.findByCollectorAndStatus(collector, null, pageable)
                .map(CollectionScheduleDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getSchedulesByCollectorAndStatus(UUID collectorId, CollectionSchedule.ScheduleStatus status) {
        log.debug("{} Buscando agendamentos por coletor ID: {} e status: {}", LOG_PREFIX, collectorId, status);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return scheduleRepository.findByCollectorAndStatus(collector, status)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getSchedulesByPoint(UUID pointId) {
        log.debug("{} Buscando agendamentos por ponto de coleta ID: {}", LOG_PREFIX, pointId);
        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado: {}", LOG_PREFIX, pointId);
                    return new ResourceNotFoundException("Collection Point", "id", pointId);
                });
        return scheduleRepository.findByCollectionPointAndStatus(point, null)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getSchedulesByDate(LocalDate date) {
        log.debug("{} Buscando agendamentos por data: {}", LOG_PREFIX, date);
        return scheduleRepository.findByScheduledDate(date)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getUpcomingSchedules(UUID collectorId) {
        log.debug("{} Buscando agendamentos futuros para coletor ID: {}", LOG_PREFIX, collectorId);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return scheduleRepository.findUpcomingByCollector(collector, LocalDate.now())
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getOverdueSchedules() {
        log.debug("{} Buscando agendamentos atrasados", LOG_PREFIX);
        return scheduleRepository.findOverdueSchedules(
                LocalDate.now(), CollectionSchedule.ScheduleStatus.PENDING)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getSchedulesByCollectorAndDateRange(
            UUID collectorId, LocalDate start, LocalDate end) {
        log.debug("{} Buscando agendamentos por coletor ID: {} entre {} e {}", LOG_PREFIX, collectorId, start, end);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return scheduleRepository.findByCollectorAndScheduledDateBetween(collector, start, end)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    // US03 - Get schedules for collector (assigned + available PENDING without collector)
    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getSchedulesForCollector(UUID collectorId) {
        log.debug("{} Buscando agendamentos para coletor (atribuídos + disponíveis) ID: {}", LOG_PREFIX, collectorId);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return scheduleRepository.findByCollectorOrAvailable(collector, CollectionSchedule.ScheduleStatus.PENDING)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    // US02 - Get schedules for establishment (only their points)
    @Transactional(readOnly = true)
    public Page<CollectionScheduleDTO> getSchedulesByEstablishment(UUID establishmentId, Pageable pageable) {
        log.debug("{} Buscando agendamentos por estabelecimento ID: {} com paginação: {}", LOG_PREFIX, establishmentId, pageable);
        User establishment = userRepository.findById(establishmentId)
                .orElseThrow(() -> {
                    log.warn("{} Estabelecimento não encontrado: {}", LOG_PREFIX, establishmentId);
                    return new ResourceNotFoundException("Establishment", "id", establishmentId);
                });
        if (establishment.getRole() != User.UserRole.ESTABLISHMENT) {
            log.warn("{} Usuário não é estabelecimento: {} (role: {})", LOG_PREFIX, establishmentId, establishment.getRole());
            throw new BusinessException("Usuário deve ter o papel de ESTABELECIMENTO", "INVALID_ESTABLISHMENT_ROLE");
        }
        return scheduleRepository.findByCollectionPoint_Establishment(establishment, pageable)
                .map(CollectionScheduleDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getSchedulesByEstablishment(UUID establishmentId) {
        log.debug("{} Buscando agendamentos por estabelecimento ID: {} como lista", LOG_PREFIX, establishmentId);
        User establishment = userRepository.findById(establishmentId)
                .orElseThrow(() -> {
                    log.warn("{} Estabelecimento não encontrado: {}", LOG_PREFIX, establishmentId);
                    return new ResourceNotFoundException("Establishment", "id", establishmentId);
                });
        if (establishment.getRole() != User.UserRole.ESTABLISHMENT) {
            log.warn("{} Usuário não é estabelecimento: {} (role: {})", LOG_PREFIX, establishmentId, establishment.getRole());
            throw new BusinessException("Usuário deve ter o papel de ESTABELECIMENTO", "INVALID_ESTABLISHMENT_ROLE");
        }
        return scheduleRepository.findByCollectionPoint_Establishment(establishment)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    // US03 - Get schedules for establishment by date
    @Transactional(readOnly = true)
    public List<CollectionScheduleDTO> getSchedulesByEstablishmentAndDate(UUID establishmentId, LocalDate date) {
        log.debug("{} Buscando agendamentos por estabelecimento ID: {} e data: {}", LOG_PREFIX, establishmentId, date);
        User establishment = userRepository.findById(establishmentId)
                .orElseThrow(() -> {
                    log.warn("{} Estabelecimento não encontrado: {}", LOG_PREFIX, establishmentId);
                    return new ResourceNotFoundException("Establishment", "id", establishmentId);
                });
        if (establishment.getRole() != User.UserRole.ESTABLISHMENT) {
            log.warn("{} Usuário não é estabelecimento: {} (role: {})", LOG_PREFIX, establishmentId, establishment.getRole());
            throw new BusinessException("Usuário deve ter o papel de ESTABELECIMENTO", "INVALID_ESTABLISHMENT_ROLE");
        }
        return scheduleRepository.findByCollectionPoint_EstablishmentAndScheduledDate(establishment, date)
                .stream().map(CollectionScheduleDTO::fromEntity).toList();
    }

    @Transactional
    public CollectionScheduleDTO createSchedule(CollectionScheduleDTO request, User createdBy, String ipAddress, String userAgent) {
        log.info("{} Criando agendamento para ponto ID: {} por usuário: {} (role: {})", LOG_PREFIX, request.getCollectionPointId(), createdBy.getEmail(), createdBy.getRole());

        CollectionPoint point = pointRepository.findById(request.getCollectionPointId())
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado: {}", LOG_PREFIX, request.getCollectionPointId());
                    return new ResourceNotFoundException("Collection Point", "id", request.getCollectionPointId());
                });

        // US02 - ESTABLISHMENT creates schedule without collector
        User collector = null;
        boolean isEstablishment = createdBy.getRole() == User.UserRole.ESTABLISHMENT;

        if (isEstablishment) {
            // Establishment can only create schedules on their own points
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(createdBy.getId())) {
                log.warn("{} Estabelecimento {} tentou criar agendamento em ponto não próprio: {}", LOG_PREFIX, createdBy.getEmail(), point.getId());
                throw new BusinessException("Você só pode criar agendamentos nos seus próprios pontos de coleta", "POINT_NOT_OWNED");
            }
            log.debug("{} Agendamento criado por estabelecimento sem coletor atribuído (status PENDING)", LOG_PREFIX);
        } else {
            // ADMIN or COLLECTOR must provide collector
            if (request.getCollectorId() == null) {
                log.warn("{} Tentativa de criar agendamento sem coletor por usuário: {} (role: {})", LOG_PREFIX, createdBy.getEmail(), createdBy.getRole());
                throw new BusinessException("Coletor é obrigatório", "COLLECTOR_REQUIRED");
            }
            collector = userRepository.findById(request.getCollectorId())
                    .orElseThrow(() -> {
                        log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, request.getCollectorId());
                        return new ResourceNotFoundException("Collector", "id", request.getCollectorId());
                    });

            if (collector.getRole() != User.UserRole.COLLECTOR) {
                log.warn("{} Usuário não é coletor: {} (role: {})", LOG_PREFIX, collector.getEmail(), collector.getRole());
                throw new BusinessException("Usuário deve ter o papel de COLETOR", "INVALID_COLLECTOR_ROLE");
            }

            if (point.getCollector() != null && !point.getCollector().getId().equals(collector.getId())) {
                log.warn("{} Ponto {} já atribuído a outro coletor: {}", LOG_PREFIX, point.getId(), point.getCollector().getId());
                throw new BusinessException("Este ponto de coleta está atribuído a outro coletor", "POINT_ASSIGNED_TO_OTHER");
            }
            log.debug("{} Agendamento criado com coletor atribuído: {}", LOG_PREFIX, collector.getEmail());
        }

        // Validar se data/hora não é no passado (fuso horário America/Sao_Paulo)
        ZoneId saoPauloZone = ZoneId.of("America/Sao_Paulo");
        LocalDateTime nowSaoPaulo = LocalDateTime.now(saoPauloZone);
        LocalDateTime scheduledDateTime = request.getScheduledDate().atTime(request.getStartTime());
        if (scheduledDateTime.isBefore(nowSaoPaulo)) {
            log.warn("{} Tentativa de criar agendamento no passado: {} (agora: {})", LOG_PREFIX, scheduledDateTime, nowSaoPaulo);
            throw new BusinessException("Não é possível criar um agendamento para uma data ou horário no passado.", "SCHEDULE_IN_PAST");
        }

        // Validate sale type and price
        if (request.getSaleType() == null) {
            request.setSaleType(CollectionSchedule.SaleType.SALE);
            log.debug("{} Tipo de venda não informado, usando padrão: SALE", LOG_PREFIX);
        }

        if (request.getSaleType() == CollectionSchedule.SaleType.SALE && request.getBasePricePerLiter() == null) {
            BigDecimal defaultPrice = systemConfigService.getBaseOilPricePerLiter();
            request.setBasePricePerLiter(defaultPrice);
            log.debug("{} Preço base não informado para venda, usando padrão: {}", LOG_PREFIX, defaultPrice);
        }

        // Calculate estimated value
        BigDecimal estimatedValue = BigDecimal.ZERO;
        if (request.getEstimatedVolumeLiters() != null && request.getBasePricePerLiter() != null) {
            estimatedValue = request.getBasePricePerLiter().multiply(BigDecimal.valueOf(request.getEstimatedVolumeLiters()));
        }

        CollectionSchedule schedule = new CollectionSchedule();
        schedule.setCollectionPoint(point);
        schedule.setCollector(collector); // Can be null for ESTABLISHMENT
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

        // Notify collector if collector exists
        if (collector != null) {
            notificationService.notifyScheduleCreated(collector.getId(), savedSchedule.getId(), point.getName());
            log.debug("{} Notificação de agendamento criada enviada para coletor: {}", LOG_PREFIX, collector.getEmail());
        }

        // Notify establishment (the creator) about their new schedule
        if (isEstablishment && point.getEstablishment() != null) {
            notificationService.notifyScheduleCreated(point.getEstablishment().getId(), savedSchedule.getId(), point.getName());
            log.debug("{} Notificação de agendamento criada enviada para estabelecimento: {}", LOG_PREFIX, point.getEstablishment().getEmail());
        }

        log.info("{} Agendamento criado: ID={}, Ponto={}, Data={}, Coletor={}, Status=PENDING", LOG_PREFIX, savedSchedule.getId(), point.getName(), request.getScheduledDate(), collector != null ? collector.getEmail() : "não atribuído");
        return CollectionScheduleDTO.fromEntity(savedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO updateSchedule(UUID id, CollectionScheduleDTO request, User updatedBy, String ipAddress, String userAgent) {
        log.info("{} Atualizando agendamento ID: {} por usuário: {}", LOG_PREFIX, id, updatedBy.getEmail());

        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado para atualização: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Schedule", "id", id);
                });

        if (schedule.getStatus() == CollectionSchedule.ScheduleStatus.COMPLETED ||
            schedule.getStatus() == CollectionSchedule.ScheduleStatus.CANCELLED) {
            log.warn("{} Tentativa de atualizar agendamento imutável: {} (status: {})", LOG_PREFIX, id, schedule.getStatus());
            throw new BusinessException("Não é possível atualizar agendamento concluído ou cancelado", "SCHEDULE_IMMUTABLE");
        }

        String oldValues = toJson(schedule);
        log.debug("{} Valores anteriores do agendamento {}: {}", LOG_PREFIX, id, oldValues);

        if (request.getScheduledDate() != null) {
            log.debug("{} Atualizando data do agendamento {}: {} -> {}", LOG_PREFIX, id, schedule.getScheduledDate(), request.getScheduledDate());
            schedule.setScheduledDate(request.getScheduledDate());
        }
        if (request.getStartTime() != null) {
            log.debug("{} Atualizando horário início do agendamento {}: {} -> {}", LOG_PREFIX, id, schedule.getStartTime(), request.getStartTime());
            schedule.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            log.debug("{} Atualizando horário fim do agendamento {}: {} -> {}", LOG_PREFIX, id, schedule.getEndTime(), request.getEndTime());
            schedule.setEndTime(request.getEndTime());
        }
        if (request.getEstimatedVolumeLiters() != null) {
            log.debug("{} Atualizando volume estimado do agendamento {}: {} -> {}", LOG_PREFIX, id, schedule.getEstimatedVolumeLiters(), request.getEstimatedVolumeLiters());
            schedule.setEstimatedVolumeLiters(request.getEstimatedVolumeLiters());
        }
        if (request.getNotes() != null) {
            log.debug("{} Atualizando observações do agendamento {}", LOG_PREFIX, id);
            schedule.setNotes(request.getNotes());
        }
        // US02, US06 - Sale/Exchange type and price
        if (request.getSaleType() != null) {
            log.debug("{} Atualizando tipo de venda do agendamento {}: {} -> {}", LOG_PREFIX, id, schedule.getSaleType(), request.getSaleType());
            schedule.setSaleType(request.getSaleType());
        }
        if (request.getTimeWindow() != null) {
            log.debug("{} Atualizando janela de tempo do agendamento {}: {} -> {}", LOG_PREFIX, id, schedule.getTimeWindow(), request.getTimeWindow());
            schedule.setTimeWindow(request.getTimeWindow());
        }
        if (request.getBasePricePerLiter() != null) {
            log.debug("{} Atualizando preço base do agendamento {}: {} -> {}", LOG_PREFIX, id, schedule.getBasePricePerLiter(), request.getBasePricePerLiter());
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

        log.info("{} Agendamento atualizado: ID={}, por={}", LOG_PREFIX, updatedSchedule.getId(), updatedBy.getEmail());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO assignSchedule(UUID id, UUID collectorId, User assignedBy, String ipAddress, String userAgent) {
        log.info("{} Atribuindo agendamento ID: {} para coletor ID: {} por usuário: {}", LOG_PREFIX, id, collectorId, assignedBy.getEmail());

        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado para atribuição: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Schedule", "id", id);
                });

        if (schedule.getStatus() != CollectionSchedule.ScheduleStatus.PENDING) {
            log.warn("{} Tentativa de atribuir agendamento com status inválido: {} (status: {})", LOG_PREFIX, id, schedule.getStatus());
            throw new BusinessException("Apenas agendamentos pendentes podem ser atribuídos", "INVALID_STATUS_TRANSITION");
        }

        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado para atribuição: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });

        if (collector.getRole() != User.UserRole.COLLECTOR) {
            log.warn("{} Usuário não é coletor: {} (role: {})", LOG_PREFIX, collector.getEmail(), collector.getRole());
            throw new BusinessException("Usuário deve ter o papel de COLETOR", "INVALID_COLLECTOR_ROLE");
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

        // Notify establishment (use point's establishment)
        if (schedule.getCollectionPoint().getEstablishment() != null) {
            notificationService.notifyScheduleAssigned(schedule.getCollectionPoint().getEstablishment().getId(),
                    updatedSchedule.getId(), collector.getName());
        }

        log.info("{} Agendamento atribuído: ID={}, Coletor={}, por={}", LOG_PREFIX, updatedSchedule.getId(), collector.getName(), assignedBy.getEmail());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO confirmSchedule(UUID id, User confirmedBy, String ipAddress, String userAgent) {
        log.info("{} Confirmando agendamento ID: {} por coletor: {}", LOG_PREFIX, id, confirmedBy.getEmail());

        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado para confirmação: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Schedule", "id", id);
                });

        // Collector can only confirm their own pending schedules
        if (schedule.getCollector() == null || !schedule.getCollector().getId().equals(confirmedBy.getId())) {
            log.warn("{} Tentativa de confirmar agendamento não atribuído ao coletor: {} (coletor do agendamento: {})", LOG_PREFIX, id, schedule.getCollector() != null ? schedule.getCollector().getId() : "null");
            throw new BusinessException("Você só pode confirmar agendamentos atribuídos a você", "UNAUTHORIZED_ACTION");
        }

        if (schedule.getStatus() != CollectionSchedule.ScheduleStatus.PENDING) {
            log.warn("{} Tentativa de confirmar agendamento com status inválido: {} (status: {})", LOG_PREFIX, id, schedule.getStatus());
            throw new BusinessException("Apenas agendamentos pendentes podem ser confirmados", "INVALID_STATUS_TRANSITION");
        }

        String oldValues = toJson(schedule);

        schedule.setStatus(CollectionSchedule.ScheduleStatus.ASSIGNED);
        schedule.setAssignedAt(LocalDateTime.now());

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), confirmedBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        // Notify establishment (use point's establishment)
        if (schedule.getCollectionPoint().getEstablishment() != null) {
            notificationService.notifyScheduleAssigned(schedule.getCollectionPoint().getEstablishment().getId(),
                    updatedSchedule.getId(), confirmedBy.getName());
        }

        log.info("{} Agendamento confirmado: ID={}, por coletor={}", LOG_PREFIX, updatedSchedule.getId(), confirmedBy.getName());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO acceptSchedule(UUID id, User acceptedBy, String ipAddress, String userAgent) {
        log.info("{} Aceitando agendamento ID: {} por coletor: {}", LOG_PREFIX, id, acceptedBy.getEmail());

        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado para aceite: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Schedule", "id", id);
                });

        // Only COLLECTOR role can accept
        if (acceptedBy.getRole() != User.UserRole.COLLECTOR) {
            log.warn("{} Tentativa de aceitar agendamento por usuário não coletor: {} (role: {})", LOG_PREFIX, acceptedBy.getEmail(), acceptedBy.getRole());
            throw new BusinessException("Apenas coletores podem aceitar coletas", "UNAUTHORIZED_ACTION");
        }

        // Schedule must be PENDING and have no collector assigned
        if (schedule.getStatus() != CollectionSchedule.ScheduleStatus.PENDING) {
            log.warn("{} Tentativa de aceitar agendamento com status inválido: {} (status: {})", LOG_PREFIX, id, schedule.getStatus());
            throw new BusinessException("Apenas agendamentos pendentes podem ser aceitos", "INVALID_STATUS_TRANSITION");
        }
        if (schedule.getCollector() != null) {
            log.warn("{} Tentativa de aceitar agendamento já atribuído: {} (coletor: {})", LOG_PREFIX, id, schedule.getCollector().getName());
            throw new BusinessException("Esta coleta já foi aceita por outro coletor", "SCHEDULE_ALREADY_ASSIGNED");
        }

        String oldValues = toJson(schedule);

        // Assign to the current collector and set status to ASSIGNED
        schedule.setCollector(acceptedBy);
        schedule.setStatus(CollectionSchedule.ScheduleStatus.ASSIGNED);
        schedule.setAssignedAt(LocalDateTime.now());

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), acceptedBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        // Notify collector who accepted the schedule
        notificationService.notifyScheduleAssigned(acceptedBy.getId(), updatedSchedule.getId(), acceptedBy.getName());

        // Notify establishment (use point's establishment)
        if (schedule.getCollectionPoint().getEstablishment() != null) {
            notificationService.notifyScheduleAssigned(schedule.getCollectionPoint().getEstablishment().getId(),
                    updatedSchedule.getId(), acceptedBy.getName());
        }

        log.info("{} Agendamento aceito: ID={}, por coletor={}", LOG_PREFIX, updatedSchedule.getId(), acceptedBy.getName());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO startSchedule(UUID id, User startedBy, String ipAddress, String userAgent) {
        log.info("{} Iniciando agendamento (em rota) ID: {} por usuário: {}", LOG_PREFIX, id, startedBy.getEmail());

        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado para início: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Schedule", "id", id);
                });

        if (schedule.getStatus() != CollectionSchedule.ScheduleStatus.ASSIGNED &&
            schedule.getStatus() != CollectionSchedule.ScheduleStatus.PENDING) {
            log.warn("{} Tentativa de iniciar agendamento com status inválido: {} (status: {})", LOG_PREFIX, id, schedule.getStatus());
            throw new BusinessException("Agendamento deve estar atribuído ou pendente para iniciar", "INVALID_STATUS_TRANSITION");
        }

        String oldValues = toJson(schedule);

        schedule.setStatus(CollectionSchedule.ScheduleStatus.EN_ROUTE);
        schedule.setEnRouteAt(LocalDateTime.now());

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), startedBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        // Notify establishment (use point's establishment)
        if (schedule.getCollectionPoint().getEstablishment() != null) {
            notificationService.notifyScheduleEnRoute(schedule.getCollectionPoint().getEstablishment().getId(), updatedSchedule.getId());
        }

        log.info("{} Agendamento iniciado (em rota): ID={}, por={}", LOG_PREFIX, updatedSchedule.getId(), startedBy.getEmail());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO arriveAtPoint(UUID id, User arrivedBy, String ipAddress, String userAgent) {
        log.info("{} Coletor chegou ao ponto - agendamento ID: {} por usuário: {}", LOG_PREFIX, id, arrivedBy.getEmail());

        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado para chegada: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Schedule", "id", id);
                });

        if (schedule.getStatus() != CollectionSchedule.ScheduleStatus.EN_ROUTE) {
            log.warn("{} Tentativa de registrar chegada com status inválido: {} (status: {})", LOG_PREFIX, id, schedule.getStatus());
            throw new BusinessException("Agendamento deve estar em rota para chegar ao ponto", "INVALID_STATUS_TRANSITION");
        }

        String oldValues = toJson(schedule);

        schedule.setStatus(CollectionSchedule.ScheduleStatus.IN_PROGRESS);

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), arrivedBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        log.info("{} Coletor chegou ao ponto: ID={}, por={}", LOG_PREFIX, updatedSchedule.getId(), arrivedBy.getEmail());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO completeSchedule(UUID id, User completedBy, String ipAddress, String userAgent) {
        log.info("{} Concluindo agendamento ID: {} por usuário: {}", LOG_PREFIX, id, completedBy.getEmail());

        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado para conclusão: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Schedule", "id", id);
                });

        if (schedule.getStatus() != CollectionSchedule.ScheduleStatus.IN_PROGRESS) {
            log.warn("{} Tentativa de concluir agendamento com status inválido: {} (status: {})", LOG_PREFIX, id, schedule.getStatus());
            throw new BusinessException("Agendamento deve estar em andamento para concluir", "INVALID_STATUS_TRANSITION");
        }

        String oldValues = toJson(schedule);

        schedule.setStatus(CollectionSchedule.ScheduleStatus.COMPLETED);
        schedule.setCompletedAt(LocalDateTime.now());

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), completedBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        // Notify collector only if collector exists
        if (schedule.getCollector() != null) {
            notificationService.notifyScheduleCompleted(schedule.getCollector().getId(), updatedSchedule.getId());
            log.debug("{} Notificação de conclusão enviada para coletor: {}", LOG_PREFIX, schedule.getCollector().getEmail());
        }

        // Notify establishment (use point's establishment)
        if (schedule.getCollectionPoint().getEstablishment() != null) {
            notificationService.notifyScheduleCompleted(schedule.getCollectionPoint().getEstablishment().getId(), updatedSchedule.getId());
            log.debug("{} Notificação de conclusão enviada para estabelecimento: {}", LOG_PREFIX, schedule.getCollectionPoint().getEstablishment().getEmail());
        }

        // Check if point is now almost full and trigger alert
        checkAndTriggerAlmostFullAlert(schedule.getCollectionPoint());

        log.info("{} Agendamento concluído: ID={}, por={}", LOG_PREFIX, updatedSchedule.getId(), completedBy.getEmail());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO cancelSchedule(UUID id, String reason, User cancelledBy, String ipAddress, String userAgent) {
        log.info("{} Cancelando agendamento ID: {} por usuário: {} (role: {}), motivo: {}", LOG_PREFIX, id, cancelledBy.getEmail(), cancelledBy.getRole(), reason);

        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado para cancelamento: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Schedule", "id", id);
                });

        if (schedule.getStatus() == CollectionSchedule.ScheduleStatus.COMPLETED) {
            log.warn("{} Tentativa de cancelar agendamento concluído: {}", LOG_PREFIX, id);
            throw new BusinessException("Não é possível cancelar agendamento concluído", "INVALID_STATUS_TRANSITION");
        }

        // US02 - Different cancellation rules for ESTABLISHMENT vs COLLECTOR
        boolean isEstablishment = cancelledBy.getRole() == User.UserRole.ESTABLISHMENT;
        boolean isCollector = cancelledBy.getRole() == User.UserRole.COLLECTOR;
        boolean isAdmin = cancelledBy.getRole() == User.UserRole.ADMIN;

        if (isEstablishment) {
            // Establishment can only cancel their own point's schedules
            if (schedule.getCollectionPoint().getEstablishment() == null ||
                !schedule.getCollectionPoint().getEstablishment().getId().equals(cancelledBy.getId())) {
                log.warn("{} Estabelecimento {} tentou cancelar agendamento de ponto não próprio: {}", LOG_PREFIX, cancelledBy.getEmail(), id);
                throw new BusinessException("Você só pode cancelar agendamentos nos seus próprios pontos de coleta", "POINT_NOT_OWNED");
            }
            // Establishment: 2-hour lock before scheduled time
            int hoursBefore = systemConfigService.getCancellationHoursBefore();
            LocalDateTime scheduledDateTime = schedule.getScheduledDate().atTime(schedule.getStartTime());
            LocalDateTime minCancellationTime = scheduledDateTime.minusHours(hoursBefore);

            // Usar fuso horário America/Sao_Paulo para comparação
            ZoneId saoPauloZone = ZoneId.of("America/Sao_Paulo");
            LocalDateTime nowSaoPaulo = LocalDateTime.now(saoPauloZone);

            if (nowSaoPaulo.isAfter(minCancellationTime)) {
                log.warn("{} Tentativa de cancelar agendamento com menos de {} horas de antecedência: {}", LOG_PREFIX, hoursBefore, id);
                throw new BusinessException(
                        "Não é possível cancelar o agendamento com menos de " + hoursBefore + " horas de antecedência.",
                        "CANCELLATION_TOO_LATE");
            }
        } else if (isCollector) {
            // Collector can only cancel their assigned schedules
            if (schedule.getCollector() == null || !schedule.getCollector().getId().equals(cancelledBy.getId())) {
                log.warn("{} Coletor {} tentou cancelar agendamento não atribuído: {}", LOG_PREFIX, cancelledBy.getEmail(), id);
                throw new BusinessException("Você só pode cancelar agendamentos atribuídos a você", "UNAUTHORIZED_ACTION");
            }
            // Collector: can cancel at any time, but reason is mandatory (already required by @RequestParam)
            if (reason == null || reason.trim().isEmpty()) {
                log.warn("{} Coletor {} tentou cancelar agendamento sem motivo: {}", LOG_PREFIX, cancelledBy.getEmail(), id);
                throw new BusinessException("Motivo do cancelamento é obrigatório para coletores", "REASON_REQUIRED");
            }
        }
        // ADMIN has no restrictions (existing behavior)

        String oldValues = toJson(schedule);

        schedule.setStatus(CollectionSchedule.ScheduleStatus.CANCELLED);
        schedule.setCancelledAt(LocalDateTime.now());
        schedule.setCancellationReason(reason);

        CollectionSchedule updatedSchedule = scheduleRepository.save(schedule);

        // Log audit
        auditLogService.logStatusChange("COLLECTION_SCHEDULE", updatedSchedule.getId(), cancelledBy,
                oldValues, toJson(updatedSchedule), ipAddress, userAgent);

        // Notify collector only if collector exists
        if (schedule.getCollector() != null) {
            notificationService.notifyScheduleCancelled(schedule.getCollector().getId(), updatedSchedule.getId(), reason);
            log.debug("{} Notificação de cancelamento enviada para coletor: {}", LOG_PREFIX, schedule.getCollector().getEmail());
        }

        // Notify establishment
        if (schedule.getCollectionPoint().getEstablishment() != null) {
            notificationService.notifyScheduleCancelled(schedule.getCollectionPoint().getEstablishment().getId(), updatedSchedule.getId(), reason);
            log.debug("{} Notificação de cancelamento enviada para estabelecimento: {}", LOG_PREFIX, schedule.getCollectionPoint().getEstablishment().getEmail());
        }

        log.info("{} Agendamento cancelado: ID={}, Motivo={}, por={}", LOG_PREFIX, updatedSchedule.getId(), reason, cancelledBy.getEmail());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public CollectionScheduleDTO rescheduleSchedule(UUID id, LocalDate newDate, LocalTime newStartTime, User rescheduledBy, String ipAddress, String userAgent) {
        log.info("{} Reagendando agendamento ID: {} para {} {} por usuário: {}", LOG_PREFIX, id, newDate, newStartTime != null ? newStartTime : "", rescheduledBy.getEmail());

        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado para reagendamento: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Schedule", "id", id);
                });

        if (schedule.getStatus() == CollectionSchedule.ScheduleStatus.COMPLETED ||
            schedule.getStatus() == CollectionSchedule.ScheduleStatus.CANCELLED) {
            log.warn("{} Tentativa de reagendar agendamento imutável: {} (status: {})", LOG_PREFIX, id, schedule.getStatus());
            throw new BusinessException("Não é possível reagendar agendamento concluído ou cancelado", "INVALID_STATUS_TRANSITION");
        }

        // Validar se nova data/hora não é no passado
        ZoneId saoPauloZone = ZoneId.of("America/Sao_Paulo");
        LocalDateTime nowSaoPaulo = LocalDateTime.now(saoPauloZone);
        LocalDateTime newScheduledDateTime = newDate.atTime(newStartTime != null ? newStartTime : schedule.getStartTime());
        if (newScheduledDateTime.isBefore(nowSaoPaulo)) {
            log.warn("{} Tentativa de reagendar para data/horário no passado: {} (agora: {})", LOG_PREFIX, newScheduledDateTime, nowSaoPaulo);
            throw new BusinessException("Não é possível reagendar para uma data ou horário no passado.", "SCHEDULE_IN_PAST");
        }

        // US03 - Validate reschedule at least 2 hours before scheduled time
        int hoursBefore = systemConfigService.getCancellationHoursBefore();
        LocalDateTime scheduledDateTime = schedule.getScheduledDate().atTime(schedule.getStartTime());
        LocalDateTime minChangeTime = scheduledDateTime.minusHours(hoursBefore);

        if (nowSaoPaulo.isAfter(minChangeTime)) {
            log.warn("{} Tentativa de reagendar com menos de {} horas de antecedência: {}", LOG_PREFIX, hoursBefore, id);
            throw new BusinessException(
                    "Não é possível reagendar com menos de " + hoursBefore + " horas de antecedência.",
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

        // Notify collector only if collector exists
        if (schedule.getCollector() != null) {
            notificationService.notifyScheduleCreated(schedule.getCollector().getId(), updatedSchedule.getId(),
                    schedule.getCollectionPoint().getName());
            log.debug("{} Notificação de reagendamento enviada para coletor: {}", LOG_PREFIX, schedule.getCollector().getEmail());
        }

        log.info("{} Agendamento reagendado: ID={}, Nova data={}, por={}", LOG_PREFIX, updatedSchedule.getId(), newDate, rescheduledBy.getEmail());
        return CollectionScheduleDTO.fromEntity(updatedSchedule);
    }

    @Transactional
    public void deleteSchedule(UUID id, User deletedBy, String ipAddress, String userAgent) {
        log.info("{} Excluindo agendamento ID: {} por usuário: {}", LOG_PREFIX, id, deletedBy.getEmail());

        CollectionSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado para exclusão: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Schedule", "id", id);
                });

        if (schedule.getStatus() == CollectionSchedule.ScheduleStatus.COMPLETED) {
            log.warn("{} Tentativa de excluir agendamento concluído: {}", LOG_PREFIX, id);
            throw new BusinessException("Não é possível excluir agendamento concluído", "SCHEDULE_IMMUTABLE");
        }

        String oldValues = toJson(schedule);

        scheduleRepository.delete(schedule);

        // Log audit
        auditLogService.logDelete("COLLECTION_SCHEDULE", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("{} Agendamento excluído: ID={}, por={}", LOG_PREFIX, id, deletedBy.getEmail());
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
                if (point.getEstablishment() != null) {
                    notificationService.notifyAlmostFull(point.getEstablishment().getId(), point.getId(), point.getName());
                }

                log.warn("{} Alerta de quase cheio disparado para ponto: {} ({}%)", LOG_PREFIX, point.getName(), fillPercentage);
            } else if (fillPercentage < 70 && point.getAlertAlmostFull()) {
                // Reset alert if volume drops below 70%
                point.setAlertAlmostFull(false);
                point.setAlertTriggeredAt(null);
                pointRepository.save(point);
                log.info("{} Alerta de quase cheio resetado para ponto: {} ({}%)", LOG_PREFIX, point.getName(), fillPercentage);
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