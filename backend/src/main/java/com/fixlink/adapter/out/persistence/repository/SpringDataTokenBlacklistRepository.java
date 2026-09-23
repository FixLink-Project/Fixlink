package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.TokenBlacklistJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataTokenBlacklistRepository extends JpaRepository<TokenBlacklistJpaEntity, String> {

    boolean existsByJti(String jti);
}
