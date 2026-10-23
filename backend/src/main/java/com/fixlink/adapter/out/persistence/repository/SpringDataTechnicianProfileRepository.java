package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;

@Repository
public interface SpringDataTechnicianProfileRepository extends JpaRepository<TechnicianProfileJpaEntity, Long> {
    Optional<TechnicianProfileJpaEntity> findByUserId(Long userId);
    Optional<TechnicianProfileJpaEntity> findByUserIdAndDeletedAtIsNull(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TechnicianProfileJpaEntity t WHERE t.userId = :userId AND t.deletedAt IS NULL")
    Optional<TechnicianProfileJpaEntity> findByUserIdAndDeletedAtIsNullForUpdate(@Param("userId") Long userId);
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

    @Query("SELECT COUNT(DISTINCT t.userId) FROM TechnicianProfileJpaEntity t JOIN t.categoryIds c " +
            "WHERE c = :categoryId AND t.deletedAt IS NULL AND t.isOnline = true " +
            "AND t.verificationStatus = com.fixlink.domain.model.VerificationStatus.APPROVED " +
            "AND (:areaId IS NULL OR t.userId IN (SELECT t2.userId FROM TechnicianProfileJpaEntity t2 JOIN t2.areaIds a WHERE a = :areaId AND t2.deletedAt IS NULL))")
    long countAvailableByCategoryAndArea(@Param("categoryId") Long categoryId, @Param("areaId") Long areaId);

    @Query("SELECT COUNT(DISTINCT t.userId) FROM TechnicianProfileJpaEntity t JOIN t.areaIds a " +
            "WHERE a = :areaId AND t.deletedAt IS NULL AND t.isOnline = true " +
            "AND t.verificationStatus = com.fixlink.domain.model.VerificationStatus.APPROVED")
    long countAvailableByArea(@Param("areaId") Long areaId);

    @Query("SELECT c, COUNT(DISTINCT t.userId) FROM TechnicianProfileJpaEntity t JOIN t.categoryIds c " +
            "WHERE t.deletedAt IS NULL AND t.isOnline = true " +
            "AND t.verificationStatus = com.fixlink.domain.model.VerificationStatus.APPROVED " +
            "AND (:areaId IS NULL OR t.userId IN (SELECT t2.userId FROM TechnicianProfileJpaEntity t2 JOIN t2.areaIds a WHERE a = :areaId AND t2.deletedAt IS NULL)) " +
            "GROUP BY c")
    List<Object[]> countAvailableByCategoryGrouped(@Param("areaId") Long areaId);

    @Query("SELECT a, COUNT(DISTINCT t.userId) FROM TechnicianProfileJpaEntity t JOIN t.areaIds a " +
            "WHERE t.deletedAt IS NULL AND t.isOnline = true " +
            "AND t.verificationStatus = com.fixlink.domain.model.VerificationStatus.APPROVED GROUP BY a")
    List<Object[]> countAvailableByAreaGrouped();

    @Query("SELECT DISTINCT t FROM TechnicianProfileJpaEntity t JOIN t.areaIds a " +
            "WHERE a = :areaId AND t.deletedAt IS NULL AND t.isOnline = false " +
            "AND t.verificationStatus = com.fixlink.domain.model.VerificationStatus.APPROVED " +
            "AND t.user.status = com.fixlink.domain.model.UserStatus.ACTIVE " +
            "AND (:categoryId IS NULL OR :categoryId MEMBER OF t.categoryIds) " +
            "ORDER BY t.avgRating DESC, t.completedJobs DESC")
    List<TechnicianProfileJpaEntity> findVerifiedOfflineByAreaAndCategory(
            @Param("areaId") Long areaId, @Param("categoryId") Long categoryId);
}
