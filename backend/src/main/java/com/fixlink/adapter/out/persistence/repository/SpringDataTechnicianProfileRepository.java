package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataTechnicianProfileRepository extends JpaRepository<TechnicianProfileJpaEntity, Long> {

    Optional<TechnicianProfileJpaEntity> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<TechnicianProfileJpaEntity> findByUserId(Long userId);

    Optional<TechnicianProfileJpaEntity> findByEmail(String email);

    Optional<TechnicianProfileJpaEntity> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhoneOrEmailOrCitizenIdAndDeletedAtIsNull(String phone, String email, String citizenId);


    boolean existsByPhoneAndUserIdNot(String phone, Long userId);

    boolean existsByEmailAndUserIdNot(String email, Long userId);
}
