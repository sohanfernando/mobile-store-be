package com.mobilestore.mobile_store.service.impl;

import com.mobilestore.mobile_store.dto.request.CreateProductRequestDto;
import com.mobilestore.mobile_store.dto.request.UpdateProductRequestDto;
import com.mobilestore.mobile_store.dto.response.ProductResponseDto;
import com.mobilestore.mobile_store.entity.Product;
import com.mobilestore.mobile_store.entity.ProductColorVariant;
import com.mobilestore.mobile_store.exception.ProductInUseException;
import com.mobilestore.mobile_store.exception.ProductNotFoundException;
import com.mobilestore.mobile_store.exception.ProductVariantInUseException;
import com.mobilestore.mobile_store.mapper.ProductMapper;
import com.mobilestore.mobile_store.repository.ProductRepository;
import com.mobilestore.mobile_store.service.ProductService;
import com.mobilestore.mobile_store.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;

    @Override
    public ProductResponseDto createProduct(CreateProductRequestDto request){
        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponseDto(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toResponseDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts(){
        return productRepository.findAll().stream()
                .map(productMapper::toResponseDto)
                .toList();
    }

    @Override
    public ProductResponseDto updateProduct(Long id, UpdateProductRequestDto request){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        Set<String> imagesBeforeUpdate = collectImageUrls(product);

        productMapper.updateEntityFromRequest(request, product);
        try {
            Product updatedProduct = productRepository.save(product);
            productRepository.flush();

            Set<String> removedImages = new HashSet<>(imagesBeforeUpdate);
            removedImages.removeAll(collectImageUrls(updatedProduct));
            removedImages.forEach(fileStorageService::delete);

            return productMapper.toResponseDto(updatedProduct);
        } catch (DataIntegrityViolationException ex) {
            throw new ProductVariantInUseException();
        }
    }

    @Override
    public void deleteProduct(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        Set<String> images = collectImageUrls(product);

        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ProductInUseException(id);
        }

        images.forEach(fileStorageService::delete);
    }

    private Set<String> collectImageUrls(Product product) {
        Set<String> urls = new HashSet<>();
        if (product.getDescriptionImageUrl() != null) {
            urls.add(product.getDescriptionImageUrl());
        }
        if (product.getColorVariants() != null) {
            for (ProductColorVariant variant : product.getColorVariants()) {
                if (variant.getImages() != null) {
                    urls.addAll(variant.getImages());
                }
            }
        }
        return urls;
    }
}
