package com.oilcollection.repository;

import com.oilcollection.model.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByCollectionPointId(UUID collectionPointId);

    List<Incident> findByScheduleId(UUID scheduleId);

    List<Incident> findByReporterId(UUID reporterId);

    List<Incident> findByStatus(String status);

    Page<Incident> findByStatus(String status, Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE i.collectionPointId = :pointId AND i.reportedAt >= :since")
    List<Incident> findRecentByCollectionPoint(@Param("pointId") UUID pointId, @Param("since") LocalDateTime since);
}