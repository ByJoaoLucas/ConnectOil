package com.oilcollection.service;

import com.oilcollection.dto.ExchangeItemDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.CollectionSchedule;
import com.oilcollection.model.ExchangeItem;
import com.oilcollection.model.Product;
import com.oilcollection.model.User;
import com.oilcollection.repository.CollectionScheduleRepository;
import com.oilcollection.repository.ExchangeItemRepository;
import com.oilcollection.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeItemService {

    private final ExchangeItemRepository exchangeItemRepository;
    private final ProductRepository productRepository;
    private final CollectionScheduleRepository scheduleRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ExchangeItemDTO> getByScheduleId(UUID scheduleId) {
        return exchangeItemRepository.findByScheduleId(scheduleId).stream()
                .map(ExchangeItemDTO::fromEntity)
                .toList();
    }

    @Transactional
    public List<ExchangeItemDTO> createForSchedule(UUID scheduleId, List<ExchangeItemDTO.ExchangeItemRequestDTO> requests, User createdBy, String ipAddress, String userAgent) {
        CollectionSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        if (schedule.getSaleType() != CollectionSchedule.SaleType.EXCHANGE) {
            throw new BusinessException("Exchange items can only be added to EXCHANGE type schedules", "INVALID_SALE_TYPE");
        }

        for (ExchangeItemDTO.ExchangeItemRequestDTO request : requests) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

            if (!product.getIsActive()) {
                throw new BusinessException("Product is not active: " + product.getName(), "PRODUCT_INACTIVE");
            }

            if (product.getStockQuantity() < request.getQuantity()) {
                throw new BusinessException("Insufficient stock for: " + product.getName(), "INSUFFICIENT_STOCK");
            }

            ExchangeItem item = ExchangeItem.builder()
                    .schedule(schedule)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPricePerUnit())
                    .totalPrice(product.getPricePerUnit().multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();

            exchangeItemRepository.save(item);

            // Update stock
            product.setStockQuantity(product.getStockQuantity() - request.getQuantity());
            productRepository.save(product);

            // Log audit
            auditLogService.logCreate("EXCHANGE_ITEM", item.getId(), createdBy,
                    toJson(item), ipAddress, userAgent);
        }

        // Notify exchange products ready
        // notificationService.notifyExchangeProductsReady(schedule.getCollector().getId(), scheduleId);

        log.info("Exchange items created for schedule: {}", scheduleId);
        return getByScheduleId(scheduleId);
    }

    @Transactional
    public void deleteByScheduleId(UUID scheduleId, User deletedBy, String ipAddress, String userAgent) {
        List<ExchangeItem> items = exchangeItemRepository.findByScheduleId(scheduleId);
        for (ExchangeItem item : items) {
            String oldValues = toJson(item);

            // Restore stock
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);

            // Log audit
            auditLogService.logDelete("EXCHANGE_ITEM", item.getId(), deletedBy,
                    oldValues, ipAddress, userAgent);
        }
        exchangeItemRepository.deleteAll(items);

        log.info("Exchange items deleted for schedule: {}", scheduleId);
    }

    private String toJson(ExchangeItem item) {
        return String.format(
                "{\"id\":\"%s\",\"product\":\"%s\",\"quantity\":%d,\"unitPrice\":%s,\"totalPrice\":%s}",
                item.getId(),
                item.getProduct() != null ? item.getProduct().getName() : "null",
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }
}