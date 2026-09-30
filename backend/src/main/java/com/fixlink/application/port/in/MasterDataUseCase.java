package com.fixlink.application.port.in;

import com.fixlink.domain.model.Service;
import com.fixlink.domain.model.ServiceCategory;

import java.util.List;

/**
 * Cổng vào (port in) cho các truy vấn dữ liệu master công khai sau khi đăng nhập:
 * danh mục ngành nghề, gói dịch vụ và địa bàn hoạt động.
 *
 * <p>Tồn tại để tầng controller không còn gọi thẳng Spring Data repository (vi
 * phạm quy tắc hexagonal): controller chỉ phụ thuộc use case này.
 */
public interface MasterDataUseCase {

    /** Danh mục ngành nghề đang hoạt động, sắp theo displayOrder. */
    List<ServiceCategory> getActiveCategories();

    /** Gói dịch vụ đang hoạt động; lọc theo {@code categoryId} nếu khác null. */
    List<Service> getServices(Long categoryId);

    /** Địa bàn hoạt động đang mở. */
    List<ServiceAreaView> getActiveAreas();

    /** View gọn của một địa bàn phục vụ (chưa có domain model riêng cho area). */
    record ServiceAreaView(Long id, String code, String name, String city) {
    }
}
