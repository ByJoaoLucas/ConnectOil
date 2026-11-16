package com.oilcollection.repository;

import com.oilcollection.dto.DailyVolumeDTO;
import com.oilcollection.dto.CollectorPerformanceDTO;
import com.oilcollection.dto.OilTypeVolumeDTO;
import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.CollectionRecord;
import com.oilcollection.model.CollectionSchedule;
import com.oilcollection.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CollectionRecordRepository extends JpaRepository<CollectionRecord, UUID> {

    boolean existsByConfirmationCode(String confirmationCode);

    boolean existsByCertificateCode(String certificateCode);

    List<CollectionRecord> findByCollectionPoint(CollectionPoint point);

    Page<CollectionRecord> findByCollectionPoint(CollectionPoint point, Pageable pageable);

    List<CollectionRecord> findByCollector(User collector);

    Page<CollectionRecord> findByCollector(User collector, Pageable pageable);

    List<CollectionRecord> findBySchedule(CollectionSchedule schedule);

    List<CollectionRecord> findByCollectionDateBetween(LocalDateTime start, LocalDateTime end);

    Page<CollectionRecord> findByCollectionDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT r FROM CollectionRecord r WHERE r.collectionDate >= :startDate AND r.collectionDate <= :endDate AND r.collector = :collector")
    List<CollectionRecord> findByCollectorAndDateRange(
            @Param("collector") User collector,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT SUM(r.volumeCollectedLiters) FROM CollectionRecord r WHERE r.collector = :collector AND r.collectionDate BETWEEN :start AND :end")
    BigDecimal getTotalVolumeByCollectorAndPeriod(
            @Param("collector") User collector,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT SUM(r.volumeCollectedLiters) FROM CollectionRecord r WHERE r.collectionPoint = :point AND r.collectionDate BETWEEN :start AND :end")
    BigDecimal getTotalVolumeByPointAndPeriod(
            @Param("point") CollectionPoint point,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(r) FROM CollectionRecord r WHERE r.collector = :collector AND r.collectionDate BETWEEN :start AND :end")
    long countByCollectorAndPeriod(
            @Param("collector") User collector,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT AVG(r.qualityRating) FROM CollectionRecord r WHERE r.collector = :collector AND r.qualityRating IS NOT NULL AND r.collectionDate BETWEEN :start AND :end")
    Double getAverageQualityByCollectorAndPeriod(
            @Param("collector") User collector,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<CollectionRecord> findByVerifiedByManagerFalse();

    Page<CollectionRecord> findByVerifiedByManagerFalse(Pageable pageable);

    @Query("SELECT r FROM CollectionRecord r WHERE r.verifiedByManager = false AND r.collectionPoint.collector = :collector")
    List<CollectionRecord> findPendingVerificationByCollector(@Param("collector") User collector);

    @Query("SELECT NEW com.oilcollection.dto.DailyVolumeDTO(DATE(r.collectionDate), SUM(r.volumeCollectedLiters)) " +
           "FROM CollectionRecord r WHERE r.collectionDate BETWEEN :start AND :end GROUP BY DATE(r.collectionDate) ORDER BY DATE(r.collectionDate)")
    List<DailyVolumeDTO> getDailyVolumeReport(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT NEW com.oilcollection.dto.CollectorPerformanceDTO(r.collector.id, r.collector.name, COUNT(r), SUM(r.volumeCollectedLiters), AVG(r.qualityRating)) " +
           "FROM CollectionRecord r WHERE r.collectionDate BETWEEN :start AND :end GROUP BY r.collector.id, r.collector.name")
    List<CollectorPerformanceDTO> getCollectorPerformanceReport(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT NEW com.oilcollection.dto.OilTypeVolumeDTO(r.oilType, SUM(r.volumeCollectedLiters)) " +
           "FROM CollectionRecord r WHERE r.collectionDate BETWEEN :start AND :end AND r.oilType IS NOT NULL GROUP BY r.oilType")
    List<OilTypeVolumeDTO> getOilTypeVolumeReport(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}