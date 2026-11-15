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

    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable).map(ProductDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findByIsActiveTrue().stream().map(ProductDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(UUID id) {
        return productRepository.findById(id)
                .map(ProductDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAvailableProducts() {
        return productRepository.findAvailableProducts().stream().map(ProductDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndIsActiveTrue(category).stream().map(ProductDTO::fromEntity).toList();
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO request, User createdBy, String ipAddress, String userAgent) {
        if (productRepository.existsByName(request.getName())) {
            throw new BusinessException("Product with this name already exists", "PRODUCT_NAME_EXISTS");
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

        log.info("Product created: {} by {}", savedProduct.getName(), createdBy.getEmail());
        return ProductDTO.fromEntity(savedProduct);
    }

    @Transactional
    public ProductDTO updateProduct(UUID id, ProductDTO request, User updatedBy, String ipAddress, String userAgent) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        String oldValues = toJson(existing);

        if (!existing.getName().equals(request.getName()) && productRepository.existsByName(request.getName())) {
            throw new BusinessException("Product with this name already exists", "PRODUCT_NAME_EXISTS");
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

        log.info("Product updated: {} by {}", updatedProduct.getName(), updatedBy.getEmail());
        return ProductDTO.fromEntity(updatedProduct);
    }

    @Transactional
    public ProductDTO updateStock(UUID id, int quantityChange, User updatedBy, String ipAddress, String userAgent) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        String oldValues = toJson(product);

        int newStock = product.getStockQuantity() + quantityChange;
        if (newStock < 0) {
            throw new BusinessException("Insufficient stock", "INSUFFICIENT_STOCK");
        }
        product.setStockQuantity(newStock);

        Product updatedProduct = productRepository.save(product);

        // Log audit
        auditLogService.logUpdate("PRODUCT", updatedProduct.getId(), updatedBy,
                oldValues, toJson(updatedProduct), ipAddress, userAgent);

        log.info("Product stock updated: {} new stock={} by {}", product.getName(), newStock, updatedBy.getEmail());
        return ProductDTO.fromEntity(updatedProduct);
    }

    @Transactional
    public void deleteProduct(UUID id, User deletedBy, String ipAddress, String userAgent) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        String oldValues = toJson(product);

        productRepository.delete(product);

        // Log audit
        auditLogService.logDelete("PRODUCT", id, deletedBy,
                oldValues, ipAddress, userAgent);

        log.info("Product deleted: {} by {}", product.getName(), deletedBy.getEmail());
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