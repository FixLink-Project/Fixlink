package com.fixlink.application.port.out;

import com.fixlink.application.port.in.MasterDataUseCase.ServiceAreaView;
import com.fixlink.domain.model.Service;
import com.fixlink.domain.model.ServiceCategory;

import java.util.List;

/**
 * Cổng ra (port out) cho dữ liệu master. Application service chỉ phụ thuộc cổng
 * này (làm việc với domain model), phần map JPA entity ↔ domain nằm ở adapter.
 */
public interface MasterDataRepositoryPort {

    List<ServiceCategory> findActiveCategories();

    List<Service> findServices(Long categoryId);

    List<ServiceAreaView> findActiveAreas();
}
