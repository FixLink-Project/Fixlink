package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;
import com.fixlink.domain.model.Role;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long>, JpaSpecificationExecutor<UserJpaEntity> {
    Optional<UserJpaEntity> findByUsername(String username);
    Optional<UserJpaEntity> findByUsernameAndDeletedAtIsNull(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndDeletedAtIsNull(String username);
    Optional<UserJpaEntity> findByIdAndDeletedAtIsNull(Long id);
    long countByRoleAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Role role, LocalDateTime from, LocalDateTime to);
}
