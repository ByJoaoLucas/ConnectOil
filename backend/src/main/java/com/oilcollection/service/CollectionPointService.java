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

    @Transactional(readOnly = true)
    public Page<CollectionPointDTO> getAllPoints(Pageable pageable) {
        return pointRepository.findAll(pageable).map(CollectionPointDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getAllPoints() {
        return pointRepository.findAll().stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CollectionPointDTO getPointById(UUID id) {
        CollectionPoint point = pointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", id));
        return CollectionPointDTO.fromEntity(point);
    }

    @Transactional(readOnly = true)
    public Page<CollectionPointDTO> getPointsByStatus(CollectionPoint.PointStatus status, Pageable pageable) {
        return pointRepository.findByStatus(status, pageable).map(CollectionPointDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CollectionPointDTO> getPointsByCollector(UUID collectorId, Pageable pageable) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return pointRepository.findByCollector(collector, pageable).map(CollectionPointDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getActivePointsByCollector(UUID collectorId) {
        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return pointRepository.findByStatusAndCollector(CollectionPoint.PointStatus.ACTIVE, collector)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getNearFullPoints() {
        return pointRepository.findFullOrNearFullPoints(CollectionPoint.PointStatus.ACTIVE)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getPointsByLocation(
            BigDecimal minLat, BigDecimal maxLat, BigDecimal minLon, BigDecimal maxLon) {
        return pointRepository.findByBoundingBox(minLat, maxLat, minLon, maxLon)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getPointsByCityAndState(String city, String state) {
        return pointRepository.findByCityAndStateAndStatus(city, state, CollectionPoint.PointStatus.ACTIVE)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getUnassignedPoints() {
        return pointRepository.findUnassignedPoints(CollectionPoint.PointStatus.PENDING_APPROVAL)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    // US02 - Get points for establishment (only their own)
    @Transactional(readOnly = true)
    public Page<CollectionPointDTO> getPointsByEstablishment(UUID establishmentId, Pageable pageable) {
        User establishment = userRepository.findById(establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Establishment", "id", establishmentId));
        if (establishment.getRole() != User.UserRole.ESTABLISHMENT) {
            throw new BusinessException("User must have ESTABLISHMENT role", "INVALID_ESTABLISHMENT_ROLE");
        }
        return pointRepository.findByEstablishment(establishment, pageable).map(CollectionPointDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectionPointDTO> getPointsByEstablishment(UUID establishmentId) {
        User establishment = userRepository.findById(establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Establishment", "id", establishmentId));
        if (establishment.getRole() != User.UserRole.ESTABLISHMENT) {
            throw new BusinessException("User must have ESTABLISHMENT role", "INVALID_ESTABLISHMENT_ROLE");
        }
        return pointRepository.findByEstablishment(establishment)
                .stream().map(CollectionPointDTO::fromEntity).toList();
    }

    @Transactional
    public CollectionPointDTO createPoint(CollectionPointDTO request, User createdBy, String ipAddress, String userAgent) {
        CollectionPoint point = new CollectionPoint();
        mapDTOToEntity(request, point);

        // US02 - If ESTABLISHMENT creates point, they become the owner
        if (createdBy.getRole() == User.UserRole.ESTABLISHMENT) {
            point.setEstablishment(createdBy);
        }

        CollectionPoint savedPoint = pointRepository.save(point);

        // Log audit
        auditLogService.logCreate("COLLECTION_POINT", savedPoint.getId(), createdBy,
                toJson(savedPoint), ipAddress, userAgent);

        log.info("Collection point created: {} by {}", savedPoint.getName(), createdBy.getEmail());
        return CollectionPointDTO.fromEntity(savedPoint);
    }

    @Transactional
    public CollectionPointDTO updatePoint(UUID id, CollectionPointDTO request, User updatedBy, String ipAddress, String userAgent) {
        CollectionPoint point = pointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", id));

        // US02 - ESTABLISHMENT can only update their own points
        if (updatedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(updatedBy.getId())) {
                throw new BusinessException("You can only update your own collection points", "POINT_NOT_OWNED");
            }
            // Ignore establishmentId from request body - ownership cannot be changed
        }

        String oldValues = toJson(point);

        mapDTOToEntity(request, point);

        CollectionPoint updatedPoint = pointRepository.save(point);

        // Log audit
        auditLogService.logUpdate("COLLECTION_POINT", updatedPoint.getId(), updatedBy,
                oldValues, toJson(updatedPoint), ipAddress, userAgent);

        log.info("Collection point updated: {} by {}", updatedPoint.getName(), updatedBy.getEmail());
        return CollectionPointDTO.fromEntity(updatedPoint);
    }

    @Transactional
    public void deletePoint(UUID id, User deletedBy, String ipAddress, String userAgent) {
        CollectionPoint point = pointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", id));

        // US02 - ESTABLISHMENT cannot delete points (only ADMIN can)
        if (deletedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            throw new BusinessException("Establishments cannot delete collection points", "ESTABLISHMENT_CANNOT_DELETE");
        }

        String oldValues = toJson(point);

        pointRepository.delete(point);

        // Log audit
        auditLogService.logDelete("COLLECTION_POINT", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("Collection point deleted: {} by {}", point.getName(), deletedBy.getEmail());
    }

    @Transactional
    public CollectionPointDTO assignCollector(UUID pointId, UUID collectorId, User assignedBy, String ipAddress, String userAgent) {
        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", pointId));

        // US02 - ESTABLISHMENT can only assign collector to their own points
        if (assignedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(assignedBy.getId())) {
                throw new BusinessException("You can only assign collectors to your own collection points", "POINT_NOT_OWNED");
            }
        }

        User collector = userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));

        if (collector.getRole() != User.UserRole.COLLECTOR) {
            throw new BusinessException("User must have COLLECTOR role", "INVALID_COLLECTOR_ROLE");
        }

        String oldValues = toJson(point);

        point.setCollector(collector);
        point.setStatus(CollectionPoint.PointStatus.ACTIVE);

        CollectionPoint updatedPoint = pointRepository.save(point);

        // Log audit
        auditLogService.logUpdate("COLLECTION_POINT", updatedPoint.getId(), assignedBy,
                oldValues, toJson(updatedPoint), ipAddress, userAgent);

        log.info("Collector {} assigned to point {} by {}", collector.getName(), point.getName(), assignedBy.getEmail());
        return CollectionPointDTO.fromEntity(updatedPoint);
    }

    @Transactional
    public CollectionPointDTO unassignCollector(UUID pointId, User unassignedBy, String ipAddress, String userAgent) {
        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", pointId));

        // US02 - ESTABLISHMENT can only unassign collector from their own points
        if (unassignedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(unassignedBy.getId())) {
                throw new BusinessException("You can only unassign collectors from your own collection points", "POINT_NOT_OWNED");
            }
        }

        String oldValues = toJson(point);

        point.setCollector(null);
        point.setStatus(CollectionPoint.PointStatus.PENDING_APPROVAL);

        CollectionPoint updatedPoint = pointRepository.save(point);

        // Log audit
        auditLogService.logUpdate("COLLECTION_POINT", updatedPoint.getId(), unassignedBy,
                oldValues, toJson(updatedPoint), ipAddress, userAgent);

        log.info("Collector unassigned from point {} by {}", point.getName(), unassignedBy.getEmail());
        return CollectionPointDTO.fromEntity(updatedPoint);
    }

    @Transactional
    public CollectionPointDTO updateVolume(UUID pointId, Integer volumeLiters, User updatedBy, String ipAddress, String userAgent) {
        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", pointId));

        // US02 - ESTABLISHMENT can only update volume on their own points
        if (updatedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(updatedBy.getId())) {
                throw new BusinessException("You can only update volume on your own collection points", "POINT_NOT_OWNED");
            }
        }

        if (volumeLiters < 0) {
            throw new BusinessException("Volume cannot be negative", "INVALID_VOLUME");
        }

        if (point.getCapacityLiters() != null && volumeLiters > point.getCapacityLiters()) {
            throw new BusinessException("Volume exceeds capacity", "VOLUME_EXCEEDS_CAPACITY");
        }

        String oldValues = toJson(point);

        point.setCurrentVolumeLiters(volumeLiters);

        if (point.isFull()) {
            point.setStatus(CollectionPoint.PointStatus.FULL);
        } else if (point.getStatus() == CollectionPoint.PointStatus.FULL) {
            point.setStatus(CollectionPoint.PointStatus.ACTIVE);
        }

        CollectionPoint updatedPoint = pointRepository.save(point);

        // Log audit
        auditLogService.logUpdate("COLLECTION_POINT", updatedPoint.getId(), updatedBy,
                oldValues, toJson(updatedPoint), ipAddress, userAgent);

        log.info("Volume updated for point {}: {} liters by {}", point.getName(), volumeLiters, updatedBy.getEmail());
        return CollectionPointDTO.fromEntity(updatedPoint);
    }

    @Transactional
    public CollectionPointDTO addVolume(UUID pointId, Integer additionalLiters, User updatedBy, String ipAddress, String userAgent) {
        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", pointId));

        // US02 - ESTABLISHMENT can only add volume on their own points
        if (updatedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(updatedBy.getId())) {
                throw new BusinessException("You can only add volume on your own collection points", "POINT_NOT_OWNED");
            }
        }

        int newVolume = point.getCurrentVolumeLiters() + additionalLiters;
        return updateVolume(pointId, newVolume, updatedBy, ipAddress, userAgent);
    }

    @Transactional
    public CollectionPointDTO removeVolume(UUID pointId, Integer removedLiters, User updatedBy, String ipAddress, String userAgent) {
        CollectionPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Point", "id", pointId));

        // US02 - ESTABLISHMENT can only remove volume on their own points
        if (updatedBy.getRole() == User.UserRole.ESTABLISHMENT) {
            if (point.getEstablishment() == null || !point.getEstablishment().getId().equals(updatedBy.getId())) {
                throw new BusinessException("You can only remove volume on your own collection points", "POINT_NOT_OWNED");
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