package com.oilcollection.dto;

import com.oilcollection.model.SystemConfig;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SystemConfigDTO {
    private UUID id;

    private String configKey;

    private String configValue;

    private String description;

    private String dataType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static SystemConfigDTO fromEntity(SystemConfig config) {
        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setId(config.getId());
        dto.setConfigKey(config.getConfigKey());
        dto.setConfigValue(config.getConfigValue());
        dto.setDescription(config.getDescription());
        dto.setDataType(config.getDataType());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }
}