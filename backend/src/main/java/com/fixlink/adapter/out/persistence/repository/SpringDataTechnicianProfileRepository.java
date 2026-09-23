package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataTechnicianProfileRepository extends JpaRepository<TechnicianProfileJpaEntity, Long> {
    Optional<TechnicianProfileJpaEntity> findByUserId(Long userId);
    Optional<TechnicianProfileJpaEntity> findByUserIdAndDeletedAtIsNull(Long userId);
    Optional<TechnicianProfileJpaEntity> findByEmailAndDeletedAtIsNull(String email);
    Optional<TechnicianProfileJpaEntity> findByEmail(String email);
    Optional<TechnicianProfileJpaEntity> findByPhoneAndDeletedAtIsNull(String phone);
    Optional<TechnicianProfileJpaEntity> findByCitizenIdAndDeletedAtIsNull(String citizenId);

    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByCitizenId(String citizenId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TechnicianProfileJpaEntity t WHERE (t.phone = :phone OR t.email = :email OR t.citizenId = :citizenId) AND t.deletedAt IS NULL")
    boolean existsByPhoneOrEmailOrCitizenId(@Param("phone") String phone, @Param("email") String email, @Param("citizenId") String citizenId);

    boolean existsByPhoneAndUserIdNot(String phone, Long userId);
    boolean existsByEmailAndUserIdNot(String email, Long userId);
    boolean existsByPhoneAndUserIdNotAndDeletedAtIsNull(String phone, Long userId);
    boolean existsByEmailAndUserIdNotAndDeletedAtIsNull(String email, Long userId);

    @Query("SELECT COUNT(t) FROM TechnicianProfileJpaEntity t JOIN t.categoryIds c WHERE c = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(t) FROM TechnicianProfileJpaEntity t JOIN t.areaIds a WHERE a = :areaId")
    long countByAreaId(@Param("areaId") Long areaId);
}
