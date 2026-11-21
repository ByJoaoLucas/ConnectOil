package com.oilcollection.service;

import com.oilcollection.dto.DashboardStatsDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final CollectionPointRepository pointRepository;
    private final CollectionScheduleRepository scheduleRepository;
    private final CollectionRecordRepository recordRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(User currentUser) {
        return switch (currentUser.getRole()) {
            case ADMIN -> getAdminDashboardStats();
            case ESTABLISHMENT -> getEstablishmentDashboardStats(currentUser);
            case COLLECTOR -> getCollectorDashboardStats(currentUser);
            default -> getDefaultDashboardStats();
        };
    }

    private DashboardStatsDTO getAdminDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        // Collection Points
        long totalPoints = pointRepository.count();
        long activePoints = pointRepository.countByStatus(CollectionPoint.PointStatus.ACTIVE);
        long fullPoints = pointRepository.countByStatus(CollectionPoint.PointStatus.FULL);
        long pendingApprovalPoints = pointRepository.countByStatus(CollectionPoint.PointStatus.PENDING_APPROVAL);

        // Schedules
        long totalSchedules = scheduleRepository.count();
        long pendingSchedules = scheduleRepository.countByStatus(CollectionSchedule.ScheduleStatus.PENDING);
        long confirmedSchedules = scheduleRepository.countByStatus(CollectionSchedule.ScheduleStatus.CONFIRMED);
        long todaySchedules = scheduleRepository.countByScheduledDate(today);
        long overdueSchedules = scheduleRepository.countOverdueSchedules(today, CollectionSchedule.ScheduleStatus.PENDING);

        // Records
        long totalRecords = recordRepository.count();
        long pendingVerification = recordRepository.countByVerifiedByManagerFalse();
        long monthRecords = recordRepository.countByCollectionDateAfter(startOfMonth);
        BigDecimal totalVolume = recordRepository.getTotalVolumeAllTime();
        BigDecimal monthVolume = recordRepository.getTotalVolumeByPeriod(startOfMonth, LocalDateTime.now());

        // Collectors
        long totalCollectors = userRepository.countByRole(User.UserRole.COLLECTOR);
        long activeCollectors = userRepository.countByRoleAndIsActive(User.UserRole.COLLECTOR, true);

        // Establishments
        long totalEstablishments = userRepository.countByRole(User.UserRole.ESTABLISHMENT);
        long activeEstablishments = userRepository.countByRoleAndIsActive(User.UserRole.ESTABLISHMENT, true);

        return DashboardStatsDTO.builder()
                // Points
                .totalPoints(totalPoints)
                .activePoints(activePoints)
                .fullPoints(fullPoints)
                .pendingApprovalPoints(pendingApprovalPoints)
                // Schedules
                .totalSchedules(totalSchedules)
                .pendingSchedules(pendingSchedules)
                .confirmedSchedules(confirmedSchedules)
                .todaySchedules(todaySchedules)
                .overdueSchedules(overdueSchedules)
                // Records
                .totalRecords(totalRecords)
                .pendingVerification(pendingVerification)
                .monthRecords(monthRecords)
                .totalVolume(totalVolume)
                .monthVolume(monthVolume)
                // Users
                .totalCollectors(totalCollectors)
                .activeCollectors(activeCollectors)
                .totalEstablishments(totalEstablishments)
                .activeEstablishments(activeEstablishments)
                .build();
    }

    private DashboardStatsDTO getEstablishmentDashboardStats(User establishmentUser) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        // Get collection points for this establishment (by email match)
        List<CollectionPoint> establishmentPoints = pointRepository.findByContactEmail(establishmentUser.getEmail());

        // Collection Points
        long totalPoints = establishmentPoints.size();
        long activePoints = establishmentPoints.stream()
                .filter(p -> p.getStatus() == CollectionPoint.PointStatus.ACTIVE)
                .count();
        long fullPoints = establishmentPoints.stream()
                .filter(p -> p.getStatus() == CollectionPoint.PointStatus.FULL)
                .count();

        // Schedules for these points
        List<CollectionSchedule> pointSchedules = scheduleRepository.findByCollectionPointIn(establishmentPoints);
        long totalSchedules = pointSchedules.size();
        long pendingSchedules = pointSchedules.stream()
                .filter(s -> s.getStatus() == CollectionSchedule.ScheduleStatus.PENDING ||
                           s.getStatus() == CollectionSchedule.ScheduleStatus.CONFIRMED)
                .count();
        long todaySchedules = pointSchedules.stream()
                .filter(s -> s.getScheduledDate().equals(today))
                .count();

        // Records for these points
        List<CollectionRecord> pointRecords = recordRepository.findByCollectionPointIn(establishmentPoints);
        long totalRecords = pointRecords.size();
        long monthRecords = pointRecords.stream()
                .filter(r -> r.getCollectionDate().isAfter(startOfMonth))
                .count();
        BigDecimal totalVolume = pointRecords.stream()
                .map(CollectionRecord::getVolumeCollectedLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthVolume = pointRecords.stream()
                .filter(r -> r.getCollectionDate().isAfter(startOfMonth))
                .map(CollectionRecord::getVolumeCollectedLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingVerification = pointRecords.stream()
                .filter(r -> !r.getVerifiedByManager())
                .count();

        return DashboardStatsDTO.builder()
                // Points
                .totalPoints(totalPoints)
                .activePoints(activePoints)
                .fullPoints(fullPoints)
                .pendingApprovalPoints(0L)
                // Schedules
                .totalSchedules(totalSchedules)
                .pendingSchedules(pendingSchedules)
                .confirmedSchedules(0L)
                .todaySchedules(todaySchedules)
                .overdueSchedules(0L)
                // Records
                .totalRecords(totalRecords)
                .pendingVerification(pendingVerification)
                .monthRecords(monthRecords)
                .totalVolume(totalVolume)
                .monthVolume(monthVolume)
                // Users
                .totalCollectors(0L)
                .activeCollectors(0L)
                .totalEstablishments(0L)
                .activeEstablishments(0L)
                .build();
    }

    private DashboardStatsDTO getCollectorDashboardStats(User collectorUser) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        // Schedules for this collector
        List<CollectionSchedule> collectorSchedules = scheduleRepository.findByCollector(collectorUser);
        long totalSchedules = collectorSchedules.size();
        long pendingSchedules = collectorSchedules.stream()
                .filter(s -> s.getStatus() == CollectionSchedule.ScheduleStatus.PENDING ||
                           s.getStatus() == CollectionSchedule.ScheduleStatus.CONFIRMED ||
                           s.getStatus() == CollectionSchedule.ScheduleStatus.ASSIGNED)
                .count();
        long todaySchedules = collectorSchedules.stream()
                .filter(s -> s.getScheduledDate().equals(today))
                .count();
        long inProgressSchedules = collectorSchedules.stream()
                .filter(s -> s.getStatus() == CollectionSchedule.ScheduleStatus.IN_PROGRESS ||
                           s.getStatus() == CollectionSchedule.ScheduleStatus.EN_ROUTE)
                .count();
        long completedSchedules = collectorSchedules.stream()
                .filter(s -> s.getStatus() == CollectionSchedule.ScheduleStatus.COMPLETED)
                .count();

        // Records for this collector
        List<CollectionRecord> collectorRecords = recordRepository.findByCollector(collectorUser);
        long totalRecords = collectorRecords.size();
        long monthRecords = collectorRecords.stream()
                .filter(r -> r.getCollectionDate().isAfter(startOfMonth))
                .count();
        BigDecimal totalVolume = collectorRecords.stream()
                .map(CollectionRecord::getVolumeCollectedLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthVolume = collectorRecords.stream()
                .filter(r -> r.getCollectionDate().isAfter(startOfMonth))
                .map(CollectionRecord::getVolumeCollectedLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingVerification = collectorRecords.stream()
                .filter(r -> !r.getVerifiedByManager())
                .count();

        // Assigned points
        List<CollectionPoint> assignedPoints = pointRepository.findByCollector(collectorUser);
        long totalPoints = assignedPoints.size();
        long activePoints = assignedPoints.stream()
                .filter(p -> p.getStatus() == CollectionPoint.PointStatus.ACTIVE)
                .count();
        long fullPoints = assignedPoints.stream()
                .filter(p -> p.getStatus() == CollectionPoint.PointStatus.FULL)
                .count();

        // Average quality
        double avgQuality = collectorRecords.stream()
                .filter(r -> r.getQualityRating() != null)
                .mapToInt(CollectionRecord::getQualityRating)
                .average()
                .orElse(0.0);

        return DashboardStatsDTO.builder()
                // Points
                .totalPoints(totalPoints)
                .activePoints(activePoints)
                .fullPoints(fullPoints)
                .pendingApprovalPoints(0L)
                // Schedules
                .totalSchedules(totalSchedules)
                .pendingSchedules(pendingSchedules)
                .confirmedSchedules(0L)
                .todaySchedules(todaySchedules)
                .overdueSchedules(0L)
                // Records
                .totalRecords(totalRecords)
                .pendingVerification(pendingVerification)
                .monthRecords(monthRecords)
                .totalVolume(totalVolume)
                .monthVolume(monthVolume)
                // Users
                .totalCollectors(0L)
                .activeCollectors(0L)
                .totalEstablishments(0L)
                .activeEstablishments(0L)
                // Collector specific
                .inProgressSchedules(inProgressSchedules)
                .completedSchedules(completedSchedules)
                .avgQuality(avgQuality)
                .build();
    }

    private DashboardStatsDTO getDefaultDashboardStats() {
        return DashboardStatsDTO.builder()
                .totalPoints(0L)
                .activePoints(0L)
                .fullPoints(0L)
                .pendingApprovalPoints(0L)
                .totalSchedules(0L)
                .pendingSchedules(0L)
                .confirmedSchedules(0L)
                .todaySchedules(0L)
                .overdueSchedules(0L)
                .totalRecords(0L)
                .pendingVerification(0L)
                .monthRecords(0L)
                .totalVolume(BigDecimal.ZERO)
                .monthVolume(BigDecimal.ZERO)
                .totalCollectors(0L)
                .activeCollectors(0L)
                .totalEstablishments(0L)
                .activeEstablishments(0L)
                .inProgressSchedules(0L)
                .completedSchedules(0L)
                .avgQuality(0.0)
                .build();
    }
}