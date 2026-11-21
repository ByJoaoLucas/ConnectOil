package com.oilcollection.repository;

import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.CollectionSchedule;
import com.oilcollection.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CollectionScheduleRepository extends JpaRepository<CollectionSchedule, UUID> {

    List<CollectionSchedule> findByCollectorAndStatus(User collector, CollectionSchedule.ScheduleStatus status);

    Page<CollectionSchedule> findByCollectorAndStatus(User collector, CollectionSchedule.ScheduleStatus status, Pageable pageable);

    List<CollectionSchedule> findByCollectionPointAndStatus(CollectionPoint point, CollectionSchedule.ScheduleStatus status);

    List<CollectionSchedule> findByScheduledDate(LocalDate date);

    Page<CollectionSchedule> findByScheduledDate(LocalDate date, Pageable pageable);

    List<CollectionSchedule> findByCollectorAndScheduledDateBetween(User collector, LocalDate start, LocalDate end);

    @Query("SELECT s FROM CollectionSchedule s WHERE s.scheduledDate = :date AND s.status IN :statuses")
    List<CollectionSchedule> findByDateAndStatuses(@Param("date") LocalDate date, @Param("statuses") List<CollectionSchedule.ScheduleStatus> statuses);

    @Query("SELECT s FROM CollectionSchedule s WHERE s.scheduledDate < :today AND s.status = :pendingStatus")
    List<CollectionSchedule> findOverdueSchedules(@Param("today") LocalDate today, @Param("pendingStatus") CollectionSchedule.ScheduleStatus pendingStatus);

    long countByCollectorAndStatus(User collector, CollectionSchedule.ScheduleStatus status);

    long countByCollectionPointAndStatus(CollectionPoint point, CollectionSchedule.ScheduleStatus status);

    @Query("SELECT s FROM CollectionSchedule s WHERE s.collector = :collector AND s.scheduledDate >= :today ORDER BY s.scheduledDate, s.startTime")
    List<CollectionSchedule> findUpcomingByCollector(@Param("collector") User collector, @Param("today") LocalDate today);

    @Query("SELECT s FROM CollectionSchedule s WHERE s.status = :status AND s.scheduledDate <= :date")
    List<CollectionSchedule> findPendingBeforeDate(@Param("status") CollectionSchedule.ScheduleStatus status, @Param("date") LocalDate date);

    // Dashboard queries
    List<CollectionSchedule> findByCollector(User collector);

    List<CollectionSchedule> findByCollectionPointIn(List<CollectionPoint> points);

    long countByStatus(CollectionSchedule.ScheduleStatus status);

    long countByScheduledDate(LocalDate date);

    @Query("SELECT COUNT(s) FROM CollectionSchedule s WHERE s.scheduledDate < :today AND s.status = :pendingStatus")
    long countOverdueSchedules(@Param("today") LocalDate today, @Param("pendingStatus") CollectionSchedule.ScheduleStatus pendingStatus);
}