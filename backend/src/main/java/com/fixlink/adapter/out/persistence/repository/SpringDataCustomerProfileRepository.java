package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.CustomerProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataCustomerProfileRepository extends JpaRepository<CustomerProfileJpaEntity, Long> {
    Optional<CustomerProfileJpaEntity> findByUserId(Long userId);
    Optional<CustomerProfileJpaEntity> findByUserIdAndDeletedAtIsNull(Long userId);
    Optional<CustomerProfileJpaEntity> findByEmailAndDeletedAtIsNull(String email);
    Optional<CustomerProfileJpaEntity> findByEmail(String email);
    Optional<CustomerProfileJpaEntity> findByPhoneAndDeletedAtIsNull(String phone);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CustomerProfileJpaEntity c WHERE (c.phone = :phone OR c.email = :email) AND c.deletedAt IS NULL")
    boolean existsByPhoneOrEmail(@Param("phone") String phone, @Param("email") String email);

    boolean existsByPhoneAndUserIdNotAndDeletedAtIsNull(String phone, Long userId);
    boolean existsByEmailAndUserIdNotAndDeletedAtIsNull(String email, Long userId);
}
