package com.oilcollection.repository;

import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionPointRepository extends JpaRepository<CollectionPoint, UUID> {

    List<CollectionPoint> findByStatus(CollectionPoint.PointStatus status);

    Page<CollectionPoint> findByStatus(CollectionPoint.PointStatus status, Pageable pageable);

    List<CollectionPoint> findByCollector(User collector);

    Page<CollectionPoint> findByCollector(User collector, Pageable pageable);

    List<CollectionPoint> findByStatusAndCollector(CollectionPoint.PointStatus status, User collector);

    @Query("SELECT cp FROM CollectionPoint cp WHERE cp.status = :status AND cp.currentVolumeLiters >= (cp.capacityLiters * 0.9)")
    List<CollectionPoint> findFullOrNearFullPoints(@Param("status") CollectionPoint.PointStatus status);

    @Query("SELECT cp FROM CollectionPoint cp WHERE cp.latitude BETWEEN :minLat AND :maxLat AND cp.longitude BETWEEN :minLon AND :maxLon")
    List<CollectionPoint> findByBoundingBox(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLon") BigDecimal minLon,
            @Param("maxLon") BigDecimal maxLon
    );

    @Query("SELECT cp FROM CollectionPoint cp WHERE cp.city = :city AND cp.state = :state AND cp.status = :status")
    List<CollectionPoint> findByCityAndStateAndStatus(
            @Param("city") String city,
            @Param("state") String state,
            @Param("status") CollectionPoint.PointStatus status
    );

    Optional<CollectionPoint> findByIdAndCollector(UUID id, User collector);

    long countByStatus(CollectionPoint.PointStatus status);

    long countByCollectorAndStatus(User collector, CollectionPoint.PointStatus status);

    @Query("SELECT cp FROM CollectionPoint cp WHERE cp.collector IS NULL AND cp.status = :status")
    List<CollectionPoint> findUnassignedPoints(@Param("status") CollectionPoint.PointStatus status);

    // Dashboard queries
    List<CollectionPoint> findByContactEmail(String email);

    List<CollectionPoint> findByCollectorId(UUID collectorId);

    List<CollectionPoint> findByIdIn(List<UUID> ids);

    // US02 - Find points by establishment
    Page<CollectionPoint> findByEstablishment(User establishment, Pageable pageable);

    List<CollectionPoint> findByEstablishment(User establishment);
}