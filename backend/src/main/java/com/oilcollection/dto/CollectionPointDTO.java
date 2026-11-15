package com.oilcollection.dto;

import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.User;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CollectionPointDTO {
    private UUID id;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 100, message = "Neighborhood must not exceed 100 characters")
    private String neighborhood;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 2, message = "State must be 2 characters")
    private String state;

    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "Invalid ZIP code format")
    private String zipCode;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    private CollectionPoint.PointStatus status = CollectionPoint.PointStatus.ACTIVE;

    @Min(value = 1, message = "Capacity must be at least 1 liter")
    @Max(value = 100000, message = "Capacity must not exceed 100,000 liters")
    private Integer capacityLiters;

    private Integer currentVolumeLiters = 0;

    @Size(max = 100, message = "Contact name must not exceed 100 characters")
    private String contactName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String contactPhone;

    @Email(message = "Invalid email format")
    private String contactEmail;

    private String operatingHours;

    private String acceptedOilTypes;

    private UUID collectorId;

    private String collectorName;

    public static CollectionPointDTO fromEntity(CollectionPoint point) {
        CollectionPointDTO dto = new CollectionPointDTO();
        dto.setId(point.getId());
        dto.setName(point.getName());
        dto.setDescription(point.getDescription());
        dto.setAddress(point.getAddress());
        dto.setNeighborhood(point.getNeighborhood());
        dto.setCity(point.getCity());
        dto.setState(point.getState());
        dto.setZipCode(point.getZipCode());
        dto.setLatitude(point.getLatitude());
        dto.setLongitude(point.getLongitude());
        dto.setStatus(point.getStatus());
        dto.setCapacityLiters(point.getCapacityLiters());
        dto.setCurrentVolumeLiters(point.getCurrentVolumeLiters());
        dto.setContactName(point.getContactName());
        dto.setContactPhone(point.getContactPhone());
        dto.setContactEmail(point.getContactEmail());
        dto.setOperatingHours(point.getOperatingHours());
        dto.setAcceptedOilTypes(point.getAcceptedOilTypes());
        if (point.getCollector() != null) {
            dto.setCollectorId(point.getCollector().getId());
            dto.setCollectorName(point.getCollector().getName());
        }
        return dto;
    }

    public Double getFillPercentage() {
        if (capacityLiters == null || capacityLiters == 0) return 0.0;
        return (currentVolumeLiters.doubleValue() / capacityLiters.doubleValue()) * 100;
    }
}