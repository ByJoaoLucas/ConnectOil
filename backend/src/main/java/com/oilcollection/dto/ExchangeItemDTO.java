package com.oilcollection.dto;

import com.oilcollection.model.ExchangeItem;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ExchangeItemDTO {
    private UUID id;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    private String productName;

    private String productCategory;

    private String productUnit;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    public static ExchangeItemDTO fromEntity(ExchangeItem item) {
        ExchangeItemDTO dto = new ExchangeItemDTO();
        dto.setId(item.getId());
        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
            dto.setProductCategory(item.getProduct().getCategory());
            dto.setProductUnit(item.getProduct().getUnit());
        }
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        return dto;
    }

    public static class ExchangeItemRequestDTO {
        @NotNull(message = "Product ID is required")
        private UUID productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}