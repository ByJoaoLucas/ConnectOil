package com.oilcollection.service;

import com.oilcollection.dto.CollectionPointDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.User;
import com.oilcollection.repository.CollectionPointRepository;
import com.oilcollection.repository.UserRepository;
import com.oilcollection.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionPointService {

    private final CollectionPointRepository pointRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    private static final String LOG_PREFIX = "[CollectionPointService]";

    @Transactional(readOnly = true)
    public Page<CollectionPointDTO> getAllPoints(Pageable pageable) {
        log.debug("{} Buscando todos os pontos de coleta com paginação: {}", LOG_PREFIX, pageable);
        return pointRepository.findAll(pageable).map(CollectionPointDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getAllPoints() {
        log.debug("{} Buscando todos os pontos de coleta como lista", LOG_PREFIX);
        return pointRepository.findAll().stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CollectionPointDTO getPointById(UUID id) {
        log.debug("{} Buscando ponto de coleta por ID: {}", LOG_PREFIX, id);
        CollectionPoint point = pointRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Point", "id", id);
                });
        return CollectionPointDTO.fromEntity(point);
    }

    @Transactional(readOnly = true)
    public Page<CollectionPointDTO> getPointsByStatus(CollectionPoint.PointStatus status, Pageable pageable) {
        log.debug("{} Buscando pontos de coleta por status: {} com paginação: {}", LOG_PREFIX, status, pageable);
        return pointRepository.findByStatus(status, pageable).map(CollectionPointDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CollectionPointDTO> getPointsByCollector(UUID collectorId, Pageable pageable) {
        log.debug("{} Buscando pontos por coletor ID: {} com paginação: {}", LOG_PREFIX, collectorId, pageable);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return pointRepository.findByCollector(collector, pageable).map(CollectionPointDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getActivePointsByCollector(UUID collectorId) {
        log.debug("{} Buscando pontos ativos por coletor ID: {}", LOG_PREFIX, collectorId);
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return pointRepository.findByStatusAndCollector(CollectionPoint.PointStatus.ACTIVE, collector)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getNearFullPoints() {
        log.debug("{} Buscando pontos quase cheios", LOG_PREFIX);
        return pointRepository.findFullOrNearFullPoints(CollectionPoint.PointStatus.ACTIVE)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getPointsByLocation(
            BigDecimal minLat, BigDecimal maxLat, BigDecimal minLon, BigDecimal maxLon) {
        log.debug("{} Buscando pontos por localização: lat({},{}) lon({},{})", LOG_PREFIX, minLat, maxLat, minLon, maxLon);
        return pointRepository.findByBoundingBox(minLat, maxLat, minLon, maxLon)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getPointsByCityAndState(String city, String state) {
        log.debug("{} Buscando pontos por cidade/estado: {}/{}", LOG_PREFIX, city, state);
        return pointRepository.findByCityAndStateAndStatus(city, state, CollectionPoint.PointStatus.ACTIVE)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getUnassignedPoints() {
        log.debug("{} Buscando pontos não atribuídos", LOG_PREFIX);
        return pointRepository.findUnassignedPoints(CollectionPoint.PointStatus.PENDING_APPROVAL)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    // US02 - Get points for establishment (only their own)
    @Transactional(readOnly = true)
    public Page<CollectionPointDTO> getPointsByEstablishment(UUID establishmentId, Pageable pageable) {
        log.debug("{} Buscando pontos por estabelecimento ID: {} com paginação: {}", LOG_PREFIX, establishmentId, pageable);
        User establishment = userRepository.findById(establishmentId)
                .orElseThrow(() -> {
                    log.warn("{} Estabelecimento não encontrado: {}", LOG_PREFIX, establishmentId);
                    return new ResourceNotFoundException("Establishment", "id", establishmentId);
                });
        if (establishment.getRole() != User.UserRole.ESTABLISHMENT) {
            log.warn("{} Usuário não é estabelecimento: {} (role: {})", LOG_PREFIX, establishmentId, establishment.getRole());
            throw new BusinessException("Usuário deve ter papel de ESTABELECIMENTO", "INVALID_ESTABLISHMENT_ROLE");
        }
        return pointRepository.findByEstablishment(establishment, pageable).map(CollectionPointDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getPointsByEstablishment(UUID establishmentId) {
        log.debug("{} Buscando pontos por estabelecimento ID: {} como lista", LOG_PREFIX, establishmentId);
        User establishment = userRepository.findById(establishmentId)
                .orElseThrow(() -> {
                    log.warn("{} Estabelecimento não encontrado: {}", LOG_PREFIX, establishmentId);
                    return new ResourceNotFoundException("Establishment", "id", establishmentId);
                });
        if (establishment.getRole() != User.UserRole.ESTABLISHMENT) {
            log.warn("{} Usuário não é estabelecimento: {} (role: {})", LOG_PREFIX, establishmentId, establishment.getRole());
            throw new BusinessException("Usuário deve ter papel de ESTABELECIMENTO", "INVALID_ESTABLISHMENT_ROLE");
        }
        return pointRepository.findByEstablishment(establishment)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional
    public CollectionPointDTO createPoint(CollectionPointDTO request, User createdBy, String ipAddress, String userAgent) {
        log.info("{} Criando ponto de coleta: {} por usuário: {} (role: {})", LOG_PREFIX, request.getName(), createdBy.getEmail(), createdBy.getRole());

        CollectionPoint point = new CollectionPoint();
        mapDTOToEntity(request, point);

        // US02 - If ESTABLISHMENT creates point, they become the owner
        if (createdBy.getRole() == User.UserRole.ESTABLISHMENT) {
            point.setEstablishment(createdBy);
            log.debug("{} Ponto será de propriedade do estabelecimento: {}", LOG_PREFIX, createdBy.getEmail());
        }

        CollectionPoint savedPoint = pointRepository.save(point);

        // Log audit
        auditLogService.logCreate("COLLECTION_POINT", savedPoint.getId(), createdBy,
                toJson(savedPoint), ipAddress, userAgent);

        log.info("{} Ponto de coleta criado: ID={}, Nome={}, por={}", LOG_PREFIX, savedPoint.getId(), savedPoint.getName(), createdBy.getEmail());
        return CollectionPointDTO.fromEntity(savedPoint);
    }

    @Transactional
    public CollectionPointDTO updatePoint(UUID id, CollectionPointDTO request, User updatedBy, String ipAddress, String userAgent) {
        log.info("{} Atualizando ponto de coleta ID: {} por usuário: {}", LOG_PREFIX, id, updatedBy.getEmail());

        CollectionPoint point = pointRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado para atualização: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Point", "id", id);
                });

        // US02 - ESTABLISHMENT can only update their own points
        if (updatedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(updatedBy.getId())) {
                log.warn("{} Estabelecimento {} tentou atualizar ponto não próprio: {}", LOG_PREFIX, updatedBy.getEmail(), id);
                throw new BusinessException("Você só pode atualizar seus próprios pontos de coleta", "POINT_NOT_OWNED");
            }
            // Ignore establishmentId from request body - ownership cannot be changed
        }

        String oldValues = toJson(point);
        log.debug("{} Valores anteriores do ponto {}: {}", LOG_PREFIX, id, oldValues);

        mapDTOToEntity(request, point);

        CollectionPoint updatedPoint = pointRepository.save(point);

        // Log audit
        auditLogService.logUpdate("COLLECTION_POINT", updatedPoint.getId(), updatedBy,
                oldValues, toJson(updatedPoint), ipAddress, userAgent);

        log.info("{} Ponto de coleta atualizado: ID={}, Nome={}, por={}", LOG_PREFIX, updatedPoint.getId(), updatedPoint.getName(), updatedBy.getEmail());
        return CollectionPointDTO.fromEntity(updatedPoint);
    }

    @Transactional
    public void deletePoint(UUID id, User deletedBy, String ipAddress, String userAgent) {
        log.info("{} Excluindo ponto de coleta ID: {} por usuário: {}", LOG_PREFIX, id, deletedBy.getEmail());

        CollectionPoint point = pointRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado para exclusão: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collection Point", "id", id);
                });

        // US02 - ESTABLISHMENT cannot delete points (only ADMIN can)
        if (deletedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            log.warn("{} Estabelecimento {} tentou excluir ponto de coleta: {}", LOG_PREFIX, deletedBy.getEmail(), id);
            throw new BusinessException("Estabelecimentos não podem excluir pontos de coleta", "ESTABLISHMENT_CANNOT_DELETE");
        }

        String oldValues = toJson(point);

        pointRepository.delete(point);

        // Log audit
        auditLogService.logDelete("COLLECTION_POINT", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("{} Ponto de coleta excluído: ID={}, Nome={}, por={}", LOG_PREFIX, id, point.getName(), deletedBy.getEmail());
    }

    @Transactional
    public CollectionPointDTO assignCollector(UUID pointId, UUID collectorId, User assignedBy, String ipAddress, String userAgent) {
        log.info("{} Atribuindo coletor ID: {} ao ponto ID: {} por usuário: {}", LOG_PREFIX, collectorId, pointId, assignedBy.getEmail());

        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado para atribuição: {}", LOG_PREFIX, pointId);
                    return new ResourceNotFoundException("Collection Point", "id", pointId);
                });

        // US02 - ESTABLISHMENT can only assign collector to their own points
        if (assignedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(assignedBy.getId())) {
                log.warn("{} Estabelecimento {} tentou atribuir coletor a ponto não próprio: {}", LOG_PREFIX, assignedBy.getEmail(), pointId);
                throw new BusinessException("Você só pode atribuir coletores aos seus próprios pontos de coleta", "POINT_NOT_OWNED");
            }
        }

        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });

        if (collector.getRole() != User.UserRole.COLLECTOR) {
            log.warn("{} Usuário não é coletor: {} (role: {})", LOG_PREFIX, collector.getEmail(), collector.getRole());
            throw new BusinessException("Usuário deve ter papel de COLETOR", "INVALID_COLLECTOR_ROLE");
        }

        String oldValues = toJson(point);

        point.setCollector(collector);
        point.setStatus(CollectionPoint.PointStatus.ACTIVE);

        CollectionPoint updatedPoint = pointRepository.save(point);

        // Log audit
        auditLogService.logUpdate("COLLECTION_POINT", updatedPoint.getId(), assignedBy,
                oldValues, toJson(updatedPoint), ipAddress, userAgent);

        log.info("{} Coletor {} atribuído ao ponto {} por {}", LOG_PREFIX, collector.getName(), point.getName(), assignedBy.getEmail());
        return CollectionPointDTO.fromEntity(updatedPoint);
    }

    @Transactional
    public CollectionPointDTO unassignCollector(UUID pointId, User unassignedBy, String ipAddress, String userAgent) {
        log.info("{} Desatribuindo coletor do ponto ID: {} por usuário: {}", LOG_PREFIX, pointId, unassignedBy.getEmail());

        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado para desatribuição: {}", LOG_PREFIX, pointId);
                    return new ResourceNotFoundException("Collection Point", "id", pointId);
                });

        // US02 - ESTABLISHMENT can only unassign collector from their own points
        if (unassignedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(unassignedBy.getId())) {
                log.warn("{} Estabelecimento {} tentou desatribuir coletor de ponto não próprio: {}", LOG_PREFIX, unassignedBy.getEmail(), pointId);
                throw new BusinessException("Você só pode desatribuir coletores dos seus próprios pontos de coleta", "POINT_NOT_OWNED");
            }
        }

        String oldValues = toJson(point);

        point.setCollector(null);
        point.setStatus(CollectionPoint.PointStatus.PENDING_APPROVAL);

        CollectionPoint updatedPoint = pointRepository.save(point);

        // Log audit
        auditLogService.logUpdate("COLLECTION_POINT", updatedPoint.getId(), unassignedBy,
                oldValues, toJson(updatedPoint), ipAddress, userAgent);

        log.info("{} Coletor desatribuído do ponto {} por {}", LOG_PREFIX, point.getName(), unassignedBy.getEmail());
        return CollectionPointDTO.fromEntity(updatedPoint);
    }

    @Transactional
    public CollectionPointDTO updateVolume(UUID pointId, Integer volumeLiters, User updatedBy, String ipAddress, String userAgent) {
        log.info("{} Atualizando volume do ponto ID: {} para {}L por usuário: {}", LOG_PREFIX, pointId, volumeLiters, updatedBy.getEmail());

        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado para atualização de volume: {}", LOG_PREFIX, pointId);
                    return new ResourceNotFoundException("Collection Point", "id", pointId);
                });

        // US02 - ESTABLISHMENT can only update volume on their own points
        if (updatedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(updatedBy.getId())) {
                log.warn("{} Estabelecimento {} tentou atualizar volume de ponto não próprio: {}", LOG_PREFIX, updatedBy.getEmail(), pointId);
                throw new BusinessException("Você só pode atualizar volume nos seus próprios pontos de coleta", "POINT_NOT_OWNED");
            }
        }

        if (volumeLiters < 0) {
            log.warn("{} Tentativa de definir volume negativo: {} para ponto {}", LOG_PREFIX, volumeLiters, pointId);
            throw new BusinessException("Volume não pode ser negativo", "INVALID_VOLUME");
        }

        if (point.getCapacityLiters() != null && volumeLiters > point.getCapacityLiters()) {
            log.warn("{} Tentativa de definir volume {} acima da capacidade {} para ponto {}", LOG_PREFIX, volumeLiters, point.getCapacityLiters(), pointId);
            throw new BusinessException("Volume excede a capacidade", "VOLUME_EXCEEDS_CAPACITY");
        }

        String oldValues = toJson(point);

        point.setCurrentVolumeLiters(volumeLiters);

        if (point.isFull()) {
            point.setStatus(CollectionPoint.PointStatus.FULL);
            log.debug("{} Ponto {} marcado como CHEIO", LOG_PREFIX, point.getName());
        } else if (point.getStatus() == CollectionPoint.PointStatus.FULL) {
            point.setStatus(CollectionPoint.PointStatus.ACTIVE);
            log.debug("{} Ponto {} reativado para ATIVO", LOG_PREFIX, point.getName());
        }

        CollectionPoint updatedPoint = pointRepository.save(point);

        // Log audit
        auditLogService.logUpdate("COLLECTION_POINT", updatedPoint.getId(), updatedBy,
                oldValues, toJson(updatedPoint), ipAddress, userAgent);

        log.info("{} Volume atualizado para ponto {}: {}L, Status={}, por={}", LOG_PREFIX, updatedPoint.getName(), volumeLiters, updatedPoint.getStatus(), updatedBy.getEmail());
        return CollectionPointDTO.fromEntity(updatedPoint);
    }

    @Transactional
    public CollectionPointDTO addVolume(UUID pointId, Integer additionalLiters, User updatedBy, String ipAddress, String userAgent) {
        log.info("{} Adicionando {}L ao ponto ID: {} por usuário: {}", LOG_PREFIX, additionalLiters, pointId, updatedBy.getEmail());

        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado para adição de volume: {}", LOG_PREFIX, pointId);
                    return new ResourceNotFoundException("Collection Point", "id", pointId);
                });

        // US02 - ESTABLISHMENT can only add volume on their own points
        if (updatedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(updatedBy.getId())) {
                log.warn("{} Estabelecimento {} tentou adicionar volume a ponto não próprio: {}", LOG_PREFIX, updatedBy.getEmail(), pointId);
                throw new BusinessException("Você só pode adicionar volume nos seus próprios pontos de coleta", "POINT_NOT_OWNED");
            }
        }

        int newVolume = point.getCurrentVolumeLiters() + additionalLiters;
        return updateVolume(pointId, newVolume, updatedBy, ipAddress, userAgent);
    }

    @Transactional
    public CollectionPointDTO removeVolume(UUID pointId, Integer removedLiters, User updatedBy, String ipAddress, String userAgent) {
        log.info("{} Removendo {}L do ponto ID: {} por usuário: {}", LOG_PREFIX, removedLiters, pointId, updatedBy.getEmail());

        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> {
                    log.warn("{} Ponto de coleta não encontrado para remoção de volume: {}", LOG_PREFIX, pointId);
                    return new ResourceNotFoundException("Collection Point", "id", pointId);
                });

        // US02 - ESTABLISHMENT can only remove volume on their own points
        if (updatedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(updatedBy.getId())) {
                log.warn("{} Estabelecimento {} tentou remover volume de ponto não próprio: {}", LOG_PREFIX, updatedBy.getEmail(), pointId);
                throw new BusinessException("Você só pode remover volume dos seus próprios pontos de coleta", "POINT_NOT_OWNED");
            }
        }

        int newVolume = Math.max(0, point.getCurrentVolumeLiters() - removedLiters);
        return updateVolume(pointId, newVolume, updatedBy, ipAddress, userAgent);
    }

    private void mapDTOToEntity(CollectionPointDTO dto, CollectionPoint entity) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setAddress(dto.getAddress());
        entity.setNeighborhood(dto.getNeighborhood());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZipCode(dto.getZipCode());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setStatus(dto.getStatus());
        entity.setCapacityLiters(dto.getCapacityLiters());
        entity.setCurrentVolumeLiters(dto.getCurrentVolumeLiters());
        entity.setContactName(dto.getContactName());
        entity.setContactPhone(dto.getContactPhone());
        entity.setContactEmail(dto.getContactEmail());
        entity.setOperatingHours(dto.getOperatingHours());
        entity.setAcceptedOilTypes(dto.getAcceptedOilTypes());

        if (dto.getCollectorId() != null) {
            User collector = userRepository.findById(dto.getCollectorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", dto.getCollectorId()));
            entity.setCollector(collector);
        }
    }

    private String toJson(CollectionPoint point) {
        return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"status\":\"%s\",\"city\":\"%s\",\"state\":\"%s\",\"volume\":%d,\"capacity\":%d}",
                point.getId(),
                point.getName(),
                point.getStatus(),
                point.getCity(),
                point.getState(),
                point.getCurrentVolumeLiters(),
                point.getCapacityLiters() != null ? point.getCapacityLiters() : 0
        );
    }

}