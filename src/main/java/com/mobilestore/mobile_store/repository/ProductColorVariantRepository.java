package com.mobilestore.mobile_store.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mobilestore.mobile_store.entity.ProductColorVariant;

import jakarta.persistence.LockModeType;

@Repository
public interface ProductColorVariantRepository extends JpaRepository<ProductColorVariant, Long> {

    // Acquires a row-level lock (SELECT ... FOR UPDATE) so concurrent order placements
    // for the same variant serialize instead of racing on a stale stock read.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductColorVariant v WHERE v.id = :id")
    Optional<ProductColorVariant> findByIdForUpdate(@Param("id") Long id);
}
