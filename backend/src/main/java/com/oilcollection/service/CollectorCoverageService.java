package com.oilcollection.service;

import com.oilcollection.dto.CollectorCoverageDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.CollectorCoverage;
import com.oilcollection.model.User;
import com.oilcollection.repository.CollectorCoverageRepository;
import com.oilcollection.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectorCoverageService {

    private final CollectorCoverageRepository coverageRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<CollectorCoverageDTO> getAllCoverage(Pageable pageable) {
        return coverageRepository.findAll(pageable).map(CollectorCoverageDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectorCoverageDTO> getAllCoverage() {
        return coverageRepository.findAll().stream().map(CollectorCoverageDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CollectorCoverageDTO getCoverageById(UUID id) {
        return coverageRepository.findById(id)
                .map(CollectorCoverageDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Collector Coverage", "id", id));
    }

    @Transactional(readOnly = true)
    public List<CollectorCoverageDTO> getCoverageByCollector(UUID collectorId) {
        userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return coverageRepository.findByCollectorIdAndIsActiveTrue(collectorId).stream()
                .map(CollectorCoverageDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectorCoverageDTO> getCoverageByCityAndState(String city, String state) {
        return coverageRepository.findByCityAndStateAndIsActiveTrue(city, state).stream()
                .map(CollectorCoverageDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectorCoverageDTO> getActiveCoverageByCollector(UUID collectorId) {
        userRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        return coverageRepository.findByCollectorIdAndIsActiveTrue(collectorId).stream()
                .map(CollectorCoverageDTO::fromEntity).toList();
    }

    @Transactional
    public CollectorCoverageDTO createCoverage(CollectorCoverageDTO request, User createdBy, String ipAddress, String userAgent) {
        User collector = userRepository.findById(request.getCollectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", request.getCollectorId()));

        if (collector.getRole() != User.UserRole.COLLECTOR) {
            throw new BusinessException("User must have COLLECTOR role", "INVALID_COLLECTOR_ROLE");
        }

        // Check for duplicate
        if (coverageRepository.existsByCollectorIdAndCityAndState(request.getCollectorId(), request.getCity(), request.getState())) {
            throw new BusinessException("Coverage for this collector, city, and state already exists", "COVERAGE_EXISTS");
        }

        CollectorCoverage coverage = new CollectorCoverage();
        coverage.setCollector(collector);
        coverage.setCity(request.getCity());
        coverage.setState(request.getState());
        coverage.setNeighborhoods(request.getNeighborhoods());
        coverage.setMaxDistanceKm(request.getMaxDistanceKm());
        coverage.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        CollectorCoverage savedCoverage = coverageRepository.save(coverage);

        // Log audit
        auditLogService.logCreate("COLLECTOR_COVERAGE", savedCoverage.getId(), createdBy,
                toJson(savedCoverage), ipAddress, userAgent);

        log.info("Collector coverage created: {} for collector {}", savedCoverage.getId(), collector.getName());
        return CollectorCoverageDTO.fromEntity(savedCoverage);
    }

    @Transactional
    public CollectorCoverageDTO updateCoverage(UUID id, CollectorCoverageDTO request, User updatedBy, String ipAddress, String userAgent) {
        CollectorCoverage coverage = coverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collector Coverage", "id", id));

        String oldValues = toJson(coverage);

        // Check for duplicate if city/state changed
        if (!coverage.getCity().equals(request.getCity()) || !coverage.getState().equals(request.getState())) {
            if (coverageRepository.existsByCollectorIdAndCityAndState(coverage.getCollector().getId(), request.getCity(), request.getState())) {
                throw new BusinessException("Coverage for this collector, city, and state already exists", "COVERAGE_EXISTS");
            }
        }

        coverage.setCity(request.getCity());
        coverage.setState(request.getState());
        coverage.setNeighborhoods(request.getNeighborhoods());
        coverage.setMaxDistanceKm(request.getMaxDistanceKm());
        coverage.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        CollectorCoverage updatedCoverage = coverageRepository.save(coverage);

        // Log audit
        auditLogService.logUpdate("COLLECTOR_COVERAGE", updatedCoverage.getId(), updatedBy,
                oldValues, toJson(updatedCoverage), ipAddress, userAgent);

        log.info("Collector coverage updated: {} by {}", id, updatedBy.getEmail());
        return CollectorCoverageDTO.fromEntity(updatedCoverage);
    }

    @Transactional
    public void deleteCoverage(UUID id, User deletedBy, String ipAddress, String userAgent) {
        CollectorCoverage coverage = coverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collector Coverage", "id", id));

        String oldValues = toJson(coverage);

        coverageRepository.delete(coverage);

        // Log audit
        auditLogService.logDelete("COLLECTOR_COVERAGE", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("Collector coverage deleted: {} by {}", id, deletedBy.getEmail());
    }

    private String toJson(CollectorCoverage coverage) {
        return String.format(
                "{\"id\":\"%s\",\"collector\":\"%s\",\"city\":\"%s\",\"state\":\"%s\",\"active\":%b}",
                coverage.getId(),
                coverage.getCollector() != null ? coverage.getCollector().getName() : "null",
                coverage.getCity(),
                coverage.getState(),
                coverage.getIsActive()
        );
    }
}