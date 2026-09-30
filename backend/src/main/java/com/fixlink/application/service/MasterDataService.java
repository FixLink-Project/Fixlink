package com.fixlink.application.service;

import com.fixlink.application.port.in.MasterDataUseCase;
import com.fixlink.application.port.out.MasterDataRepositoryPort;
import com.fixlink.domain.model.Service;
import com.fixlink.domain.model.ServiceCategory;
import java.util.List;

/**
 * Application service cho dữ liệu master. Chỉ phụ thuộc cổng ra
 * {@link MasterDataRepositoryPort} (domain model), không import JPA entity.
 *
 * <p>Dùng {@code @org.springframework.stereotype.Service} ở dạng fully-qualified
 * vì domain model {@link com.fixlink.domain.model.Service} trùng tên với
 * annotation của Spring.
 */
@org.springframework.stereotype.Service
public class MasterDataService implements MasterDataUseCase {

    private final MasterDataRepositoryPort masterDataRepository;

    public MasterDataService(MasterDataRepositoryPort masterDataRepository) {
        this.masterDataRepository = masterDataRepository;
    }

    @Override
    public List<ServiceCategory> getActiveCategories() {
        return masterDataRepository.findActiveCategories();
    }

    @Override
    public List<Service> getServices(Long categoryId) {
        return masterDataRepository.findServices(categoryId);
    }

    @Override
    public List<ServiceAreaView> getActiveAreas() {
        return masterDataRepository.findActiveAreas();
    }
}
