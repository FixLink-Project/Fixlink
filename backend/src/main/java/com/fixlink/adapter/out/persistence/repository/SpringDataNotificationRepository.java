package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.NotificationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, Long> {

    List<NotificationJpaEntity> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    Page<NotificationJpaEntity> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalseAndDeletedAtIsNull(Long userId);

    Optional<NotificationJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.isRead = true, n.updatedAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.isRead = false AND n.deletedAt IS NULL")
    int markAllAsReadByUserId(@Param("userId") Long userId);
}
