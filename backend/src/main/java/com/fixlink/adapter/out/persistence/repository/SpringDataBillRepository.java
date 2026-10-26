package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.BillJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataBillRepository extends JpaRepository<BillJpaEntity, Long> {

    List<BillJpaEntity> findByRequestIdAndDeletedAtIsNull(Long requestId);

    long countByBillNumberStartingWith(String prefix);

    @Query(value = "SELECT nextval('bill_daily_seq')", nativeQuery = true)
    long nextDailySequence();
}
