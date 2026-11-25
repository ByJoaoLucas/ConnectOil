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

    private static final String LOG_PREFIX = "[CollectorCoverageService]";

    @Transactional(readOnly = true)
    public Page<CollectorCoverageDTO> getAllCoverage(Pageable pageable) {
        log.debug("{} Buscando todas as áreas de cobertura com paginação: {}", LOG_PREFIX, pageable);
        return coverageRepository.findAll(pageable).map(CollectorCoverageDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CollectorCoverageDTO> getAllCoverage() {
        log.debug("{} Buscando todas as áreas de cobertura como lista", LOG_PREFIX);
        return coverageRepository.findAll().stream().map(CollectorCoverageDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CollectorCoverageDTO getCoverageById(UUID id) {
        log.debug("{} Buscando área de cobertura por ID: {}", LOG_PREFIX, id);
        return coverageRepository.findById(id)
                .map(CollectorCoverageDTO::fromEntity)
                .orElseThrow(() -> {
                    log.warn("{} Área de cobertura não encontrada: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collector Coverage", "id", id);
                });
    }

    @Transactional(readOnly = true)
    public List<CollectorCoverageDTO> getCoverageByCollector(UUID collectorId) {
        log.debug("{} Buscando áreas de cobertura por coletor ID: {}", LOG_PREFIX, collectorId);
        userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return coverageRepository.findByCollectorIdAndIsActiveTrue(collectorId).stream()
                .map(CollectorCoverageDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectorCoverageDTO> getCoverageByCityAndState(String city, String state) {
        log.debug("{} Buscando áreas de cobertura por cidade/estado: {}/{}", LOG_PREFIX, city, state);
        return coverageRepository.findByCityAndStateAndIsActiveTrue(city, state).stream()
                .map(CollectorCoverageDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CollectorCoverageDTO> getActiveCoverageByCollector(UUID collectorId) {
        log.debug("{} Buscando áreas de cobertura ativas por coletor ID: {}", LOG_PREFIX, collectorId);
        userRepository.findById(collectorId)
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado: {}", LOG_PREFIX, collectorId);
                    return new ResourceNotFoundException("Collector", "id", collectorId);
                });
        return coverageRepository.findByCollectorIdAndIsActiveTrue(collectorId).stream()
                .map(CollectorCoverageDTO::fromEntity).toList();
    }

    @Transactional
    public CollectorCoverageDTO createCoverage(CollectorCoverageDTO request, User createdBy, String ipAddress, String userAgent) {
        log.info("{} Criando área de cobertura para coletor ID: {} por usuário: {} (cidade: {}, estado: {})",
                LOG_PREFIX, request.getCollectorId(), createdBy.getEmail(), request.getCity(), request.getState());

        User collector = userRepository.findById(request.getCollectorId())
                .orElseThrow(() -> {
                    log.warn("{} Coletor não encontrado para cobertura: {}", LOG_PREFIX, request.getCollectorId());
                    return new ResourceNotFoundException("Collector", "id", request.getCollectorId());
                });

        if (collector.getRole() != User.UserRole.COLLECTOR) {
            log.warn("{} Usuário não é coletor: {} (role: {})", LOG_PREFIX, collector.getEmail(), collector.getRole());
            throw new BusinessException("Usuário deve ter papel de COLETOR", "INVALID_COLLECTOR_ROLE");
        }

        // Check for duplicate
        if (coverageRepository.existsByCollectorIdAndCityAndState(request.getCollectorId(), request.getCity(), request.getState())) {
            log.warn("{} Cobertura já existente para coletor {} na cidade/estado {}/{}", LOG_PREFIX, request.getCollectorId(), request.getCity(), request.getState());
            throw new BusinessException("Cobertura para este coletor, cidade e estado já existe", "COVERAGE_EXISTS");
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

        log.info("{} Área de cobertura criada: ID={}, Coletor={}, Cidade={}, Estado={}, Bairros={}, Distância Máx={}km, Ativo={}, por={}",
                LOG_PREFIX, savedCoverage.getId(), collector.getName(), savedCoverage.getCity(), savedCoverage.getState(), savedCoverage.getNeighborhoods(), savedCoverage.getMaxDistanceKm(), savedCoverage.getIsActive(), createdBy.getEmail());
        return CollectorCoverageDTO.fromEntity(savedCoverage);
    }

    @Transactional
    public CollectorCoverageDTO updateCoverage(UUID id, CollectorCoverageDTO request, User updatedBy, String ipAddress, String userAgent) {
        log.info("{} Atualizando área de cobertura ID: {} por usuário: {}", LOG_PREFIX, id, updatedBy.getEmail());

        CollectorCoverage coverage = coverageRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Área de cobertura não encontrada para atualização: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collector Coverage", "id", id);
                });

        String oldValues = toJson(coverage);

        // Check for duplicate if city/state changed
        if (!coverage.getCity().equals(request.getCity()) || !coverage.getState().equals(request.getState())) {
            if (coverageRepository.existsByCollectorIdAndCityAndState(coverage.getCollector().getId(), request.getCity(), request.getState())) {
                log.warn("{} Cobertura já existente para coletor {} na cidade/estado {}/{}", LOG_PREFIX, coverage.getCollector().getId(), request.getCity(), request.getState());
                throw new BusinessException("Cobertura para este coletor, cidade e estado já existe", "COVERAGE_EXISTS");
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

        log.info("{} Área de cobertura atualizada: ID={}, Coletor={}, Cidade={}, Estado={}, Bairros={}, Distância Máx={}km, Ativo={}, por={}",
                LOG_PREFIX, updatedCoverage.getId(), updatedCoverage.getCollector().getName(), updatedCoverage.getCity(), updatedCoverage.getState(), updatedCoverage.getNeighborhoods(), updatedCoverage.getMaxDistanceKm(), updatedCoverage.getIsActive(), updatedBy.getEmail());
        return CollectorCoverageDTO.fromEntity(updatedCoverage);
    }

    @Transactional
    public void deleteCoverage(UUID id, User deletedBy, String ipAddress, String userAgent) {
        log.info("{} Excluindo área de cobertura ID: {} por usuário: {}", LOG_PREFIX, id, deletedBy.getEmail());

        CollectorCoverage coverage = coverageRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Área de cobertura não encontrada para exclusão: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Collector Coverage", "id", id);
                });

        String oldValues = toJson(coverage);

        coverageRepository.delete(coverage);

        // Log audit
        auditLogService.logDelete("COLLECTOR_COVERAGE", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("{} Área de cobertura excluída: ID={}, Coletor={}, Cidade={}, Estado={}, por={}",
                LOG_PREFIX, id, coverage.getCollector().getName(), coverage.getCity(), coverage.getState(), deletedBy.getEmail());
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