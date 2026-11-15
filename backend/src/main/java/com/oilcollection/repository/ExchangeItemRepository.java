package com.oilcollection.repository;

import com.oilcollection.model.ExchangeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExchangeItemRepository extends JpaRepository<ExchangeItem, UUID> {

    List<ExchangeItem> findByScheduleId(UUID scheduleId);
}