package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, String> {

    Optional<RefreshTokenJpaEntity> findByToken(String token);

    void deleteByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshTokenJpaEntity r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
