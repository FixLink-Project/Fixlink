package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.MediaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SpringDataMediaRepository extends JpaRepository<MediaJpaEntity, Long> {
    List<MediaJpaEntity> findByOwnerTypeAndOwnerId(String ownerType, Long ownerId);

    void deleteByOwnerTypeAndOwnerId(String ownerType, Long ownerId);

    List<MediaJpaEntity> findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(String ownerType, Long ownerId);

    List<MediaJpaEntity> findByOwnerTypeAndOwnerIdInOrderByCreatedAtAsc(String ownerType, Collection<Long> ownerIds);

    long countByOwnerTypeAndOwnerId(String ownerType, Long ownerId);
}
