package com.mobilestore.mobile_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mobilestore.mobile_store.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
