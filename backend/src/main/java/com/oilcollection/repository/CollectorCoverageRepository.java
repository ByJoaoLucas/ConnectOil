package com.oilcollection.repository;

import com.oilcollection.model.CollectorCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CollectorCoverageRepository extends JpaRepository<CollectorCoverage, UUID> {

    List<CollectorCoverage> findByCollectorIdAndIsActiveTrue(UUID collectorId);

    List<CollectorCoverage> findByCityAndStateAndIsActiveTrue(String city, String state);

    @Query("SELECT c FROM CollectorCoverage c WHERE c.isActive = true AND c.city = :city AND c.state = :state")
    List<CollectorCoverage> findActiveByCityAndState(@Param("city") String city, @Param("state") String state);

    boolean existsByCollectorIdAndCityAndState(UUID collectorId, String city, String state);
}