package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.PasswordResetTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenJpaEntity, String> {

    Optional<PasswordResetTokenJpaEntity> findByToken(String token);

    @Query("SELECT COUNT(p) FROM PasswordResetTokenJpaEntity p WHERE p.email = :email AND p.createdAt >= :since")
    long countByEmailInLastHour(@Param("email") String email, @Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE PasswordResetTokenJpaEntity p SET p.isUsed = true WHERE p.user.id = :userId AND p.isUsed = false")
    void invalidateUnusedTokensByUserId(@Param("userId") Long userId);
}
