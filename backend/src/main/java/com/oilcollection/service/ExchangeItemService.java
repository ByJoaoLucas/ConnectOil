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

    private static final String LOG_PREFIX = "[ExchangeItemService]";

    @Transactional(readOnly = true)
    public List<ExchangeItemDTO> getByScheduleId(UUID scheduleId) {
        log.debug("{} Buscando itens de troca para agendamento ID: {}", LOG_PREFIX, scheduleId);
        return exchangeItemRepository.findByScheduleId(scheduleId).stream()
                .map(ExchangeItemDTO::fromEntity)
                .toList();
    }

    @Transactional
    public List<ExchangeItemDTO> createForSchedule(UUID scheduleId, List<ExchangeItemDTO.ExchangeItemRequestDTO> requests, User createdBy, String ipAddress, String userAgent) {
        log.info("{} Criando itens de troca para agendamento ID: {} por usuário: {} ({} itens)", LOG_PREFIX, scheduleId, createdBy.getEmail(), requests.size());

        CollectionSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> {
                    log.warn("{} Agendamento não encontrado: {}", LOG_PREFIX, scheduleId);
                    return new ResourceNotFoundException("Schedule", "id", scheduleId);
                });

        if (schedule.getSaleType() != CollectionSchedule.SaleType.EXCHANGE) {
            log.warn("{} Tentativa de criar itens de troca para agendamento que não é do tipo EXCHANGE: {} (tipo: {})", LOG_PREFIX, scheduleId, schedule.getSaleType());
            throw new BusinessException("Itens de troca só podem ser adicionados a agendamentos do tipo TROCA", "INVALID_SALE_TYPE");
        }

        for (ExchangeItemDTO.ExchangeItemRequestDTO request : requests) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> {
                        log.warn("{} Produto não encontrado para item de troca: {}", LOG_PREFIX, request.getProductId());
                        return new ResourceNotFoundException("Product", "id", request.getProductId());
                    });

            if (!product.getIsActive()) {
                log.warn("{} Produto inativo para item de troca: {}", LOG_PREFIX, product.getName());
                throw new BusinessException("Produto não está ativo: " + product.getName(), "PRODUCT_INACTIVE");
            }

            if (product.getStockQuantity() < request.getQuantity()) {
                log.warn("{} Estoque insuficiente para produto: {} (disponível: {}, solicitado: {})", LOG_PREFIX, product.getName(), product.getStockQuantity(), request.getQuantity());
                throw new BusinessException("Estoque insuficiente para: " + product.getName(), "INSUFFICIENT_STOCK");
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
            int oldStock = product.getStockQuantity();
            product.setStockQuantity(product.getStockQuantity() - request.getQuantity());
            productRepository.save(product);

            // Log audit
            auditLogService.logCreate("EXCHANGE_ITEM", item.getId(), createdBy,
                    toJson(item), ipAddress, userAgent);

            log.debug("{} Item de troca criado: ID={}, Produto={}, Qtd={}, Preço Unitário={}, Preço Total={}, Estoque anterior={}, Novo estoque={}",
                    LOG_PREFIX, item.getId(), product.getName(), request.getQuantity(), item.getUnitPrice(), item.getTotalPrice(), oldStock, product.getStockQuantity());
        }

        // Notify exchange products ready
        // notificationService.notifyExchangeProductsReady(schedule.getCollector().getId(), scheduleId);

        log.info("{} Itens de troca criados para agendamento: {} ({} itens)", LOG_PREFIX, scheduleId, requests.size());
        return getByScheduleId(scheduleId);
    }

    @Transactional
    public void deleteByScheduleId(UUID scheduleId, User deletedBy, String ipAddress, String userAgent) {
        log.info("{} Excluindo itens de troca para agendamento ID: {} por usuário: {}", LOG_PREFIX, scheduleId, deletedBy.getEmail());

        List<ExchangeItem> items = exchangeItemRepository.findByScheduleId(scheduleId);
        log.debug("{} Encontrados {} itens de troca para exclusão", LOG_PREFIX, items.size());

        for (ExchangeItem item : items) {
            String oldValues = toJson(item);

            // Restore stock
            Product product = item.getProduct();
            int oldStock = product.getStockQuantity();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);

            // Log audit
            auditLogService.logDelete("EXCHANGE_ITEM", item.getId(), deletedBy,
                    oldValues, ipAddress, userAgent);

            log.debug("{} Item de troca excluído: ID={}, Produto={}, Qtd restaurada={}, Estoque anterior={}, Novo estoque={}",
                    LOG_PREFIX, item.getId(), product.getName(), item.getQuantity(), oldStock, product.getStockQuantity());
        }
        exchangeItemRepository.deleteAll(items);

        log.info("{} Itens de troca excluídos para agendamento: {} ({} itens)", LOG_PREFIX, scheduleId, items.size());
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