package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.CustomerProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataCustomerProfileRepository extends JpaRepository<CustomerProfileJpaEntity, Long> {

    Optional<CustomerProfileJpaEntity> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<CustomerProfileJpaEntity> findByUserId(Long userId);

    Optional<CustomerProfileJpaEntity> findByEmail(String email);

    Optional<CustomerProfileJpaEntity> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhoneOrEmailAndDeletedAtIsNull(String phone, String email);

    boolean existsByPhoneAndUserIdNotAndDeletedAtIsNull(String phone, Long userId);

    boolean existsByEmailAndUserIdNotAndDeletedAtIsNull(String email, Long userId);
}
