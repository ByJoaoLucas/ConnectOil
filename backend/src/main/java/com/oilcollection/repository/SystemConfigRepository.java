package com.oilcollection.repository;

import com.oilcollection.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {

    Optional<SystemConfig> findByConfigKey(String configKey);

    String findConfigValueByConfigKey(String configKey);

    void deleteByConfigKey(String configKey);
}