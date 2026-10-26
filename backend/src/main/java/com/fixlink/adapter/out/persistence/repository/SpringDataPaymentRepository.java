package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.PaymentJpaEntity;
import com.fixlink.domain.model.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, Long> {

    List<PaymentJpaEntity> findByRequestIdAndDeletedAtIsNull(Long requestId);

    Optional<PaymentJpaEntity> findByRequestIdAndPaymentTypeAndDeletedAtIsNull(
            Long requestId, PaymentType paymentType);
}
