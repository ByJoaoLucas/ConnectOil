package com.oilcollection.service;

import com.oilcollection.dto.ProductDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.Product;
import com.oilcollection.model.User;
import com.oilcollection.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    private static final String LOG_PREFIX = "[ProductService]";

    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        log.debug("{} Buscando todos os produtos ativos com paginação: {}", LOG_PREFIX, pageable);
        return productRepository.findByIsActiveTrue(pageable).map(ProductDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        log.debug("{} Buscando todos os produtos ativos como lista", LOG_PREFIX);
        return productRepository.findByIsActiveTrue().stream().map(ProductDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(UUID id) {
        log.debug("{} Buscando produto por ID: {}", LOG_PREFIX, id);
        return productRepository.findById(id)
                .map(ProductDTO::fromEntity)
                .orElseThrow(() -> {
                    log.warn("{} Produto não encontrado: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Product", "id", id);
                });
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAvailableProducts() {
        log.debug("{} Buscando produtos disponíveis (estoque > minStock)", LOG_PREFIX);
        return productRepository.findAvailableProducts().stream().map(ProductDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        log.debug("{} Buscando produtos por categoria: {}", LOG_PREFIX, category);
        return productRepository.findByCategoryAndIsActiveTrue(category).stream().map(ProductDTO::fromEntity).toList();
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO request, User createdBy, String ipAddress, String userAgent) {
        log.info("{} Criando produto: {} por usuário: {}", LOG_PREFIX, request.getName(), createdBy.getEmail());

        if (productRepository.existsByName(request.getName())) {
            log.warn("{} Tentativa de criar produto com nome já existente: {}", LOG_PREFIX, request.getName());
            throw new BusinessException("Produto com este nome já existe", "PRODUCT_NAME_EXISTS");
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setUnit(request.getUnit());
        product.setPricePerUnit(request.getPricePerUnit());
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        product.setMinStockQuantity(request.getMinStockQuantity() != null ? request.getMinStockQuantity() : 0);
        product.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        product.setImageUrl(request.getImageUrl());

        Product savedProduct = productRepository.save(product);

        // Log audit
        auditLogService.logCreate("PRODUCT", savedProduct.getId(), createdBy,
                toJson(savedProduct), ipAddress, userAgent);

        log.info("{} Produto criado: ID={}, Nome={}, Categoria={}, Preço={}, Estoque={}, por={}", LOG_PREFIX, savedProduct.getId(), savedProduct.getName(), savedProduct.getCategory(), savedProduct.getPricePerUnit(), savedProduct.getStockQuantity(), createdBy.getEmail());
        return ProductDTO.fromEntity(savedProduct);
    }

    @Transactional
    public ProductDTO updateProduct(UUID id, ProductDTO request, User updatedBy, String ipAddress, String userAgent) {
        log.info("{} Atualizando produto ID: {} por usuário: {}", LOG_PREFIX, id, updatedBy.getEmail());

        Product existing = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Produto não encontrado para atualização: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Product", "id", id);
                });

        String oldValues = toJson(existing);

        if (!existing.getName().equals(request.getName()) && productRepository.existsByName(request.getName())) {
            log.warn("{} Tentativa de atualizar produto para nome já existente: {}", LOG_PREFIX, request.getName());
            throw new BusinessException("Produto com este nome já existe", "PRODUCT_NAME_EXISTS");
        }

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setCategory(request.getCategory());
        existing.setUnit(request.getUnit());
        existing.setPricePerUnit(request.getPricePerUnit());
        existing.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        existing.setMinStockQuantity(request.getMinStockQuantity() != null ? request.getMinStockQuantity() : 0);
        existing.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        existing.setImageUrl(request.getImageUrl());

        Product updatedProduct = productRepository.save(existing);

        // Log audit
        auditLogService.logUpdate("PRODUCT", updatedProduct.getId(), updatedBy,
                oldValues, toJson(updatedProduct), ipAddress, userAgent);

        log.info("{} Produto atualizado: ID={}, Nome={}, Categoria={}, Preço={}, Estoque={}, Ativo={}, por={}", LOG_PREFIX, updatedProduct.getId(), updatedProduct.getName(), updatedProduct.getCategory(), updatedProduct.getPricePerUnit(), updatedProduct.getStockQuantity(), updatedProduct.getIsActive(), updatedBy.getEmail());
        return ProductDTO.fromEntity(updatedProduct);
    }

    @Transactional
    public ProductDTO updateStock(UUID id, int quantityChange, User updatedBy, String ipAddress, String userAgent) {
        log.info("{} Atualizando estoque do produto ID: {} em {} por usuário: {}", LOG_PREFIX, id, quantityChange, updatedBy.getEmail());

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Produto não encontrado para atualização de estoque: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Product", "id", id);
                });

        String oldValues = toJson(product);

        int newStock = product.getStockQuantity() + quantityChange;
        if (newStock < 0) {
            log.warn("{} Tentativa de reduzir estoque abaixo de zero: produto={}, estoqueAtual={}, alteracao={}", LOG_PREFIX, product.getName(), product.getStockQuantity(), quantityChange);
            throw new BusinessException("Estoque insuficiente", "INSUFFICIENT_STOCK");
        }
        product.setStockQuantity(newStock);

        Product updatedProduct = productRepository.save(product);

        // Log audit
        auditLogService.logUpdate("PRODUCT", updatedProduct.getId(), updatedBy,
                oldValues, toJson(updatedProduct), ipAddress, userAgent);

        log.info("{} Estoque do produto atualizado: ID={}, Nome={}, Estoque anterior={}, Novo estoque={}, por={}", LOG_PREFIX, updatedProduct.getId(), updatedProduct.getName(), product.getStockQuantity() - quantityChange, newStock, updatedBy.getEmail());
        return ProductDTO.fromEntity(updatedProduct);
    }

    @Transactional
    public void deleteProduct(UUID id, User deletedBy, String ipAddress, String userAgent) {
        log.info("{} Excluindo produto ID: {} por usuário: {}", LOG_PREFIX, id, deletedBy.getEmail());

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Produto não encontrado para exclusão: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("Product", "id", id);
                });

        String oldValues = toJson(product);

        productRepository.delete(product);

        // Log audit
        auditLogService.logDelete("PRODUCT", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("{} Produto excluído: ID={}, Nome={}, por={}", LOG_PREFIX, id, product.getName(), deletedBy.getEmail());
    }

    private String toJson(Product product) {
        return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"category\":\"%s\",\"price\":%s,\"stock\":%d,\"active\":%b}",
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPricePerUnit(),
                product.getStockQuantity(),
                product.getIsActive()
        );
    }
}