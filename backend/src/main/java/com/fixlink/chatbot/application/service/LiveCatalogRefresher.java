package com.fixlink.chatbot.application.service;

import com.fixlink.application.port.in.MasterDataUseCase;
import com.fixlink.chatbot.domain.service.LiveCatalog;
import com.fixlink.chatbot.domain.service.ServiceCatalog.Item;
import com.fixlink.domain.model.Service;
import com.fixlink.domain.model.ServiceCategory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Đồng bộ dữ liệu dịch vụ (danh mục, gói, giá, thời gian, khu vực) từ database vào {@link LiveCatalog} để mọi câu trả lời
 * của chatbot (câu trả lời chuẩn, bảng giá, nhận diện thiết bị/khu vực, prompt AI) luôn khớp dữ liệu mới nhất: admin thêm
 * danh mục, đổi giá hay mở khu vực mới thì tối đa {@link #MAX_AGE_MS} sau chatbot đã trả lời theo dữ liệu mới.
 */
@Component
public class LiveCatalogRefresher {

    private static final Logger LOG = LoggerFactory.getLogger(LiveCatalogRefresher.class);
    static final long MAX_AGE_MS = 60_000;

    private final MasterDataUseCase masterData;
    private volatile long loadedAt;

    public LiveCatalogRefresher(MasterDataUseCase masterData) {
        this.masterData = masterData;
    }

    /** Gọi đầu mỗi lượt chat: chỉ đọc lại database khi bản chụp đã cũ hơn 60 giây. */
    public void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (now - loadedAt < MAX_AGE_MS) {
            return;
        }
        synchronized (this) {
            if (System.currentTimeMillis() - loadedAt < MAX_AGE_MS) {
                return;
            }
            refresh();
            loadedAt = System.currentTimeMillis();
        }
    }

    /** Đọc database ngay; lỗi thì giữ bản chụp cũ (chatbot vẫn trả lời bằng dữ liệu gần nhất). */
    public void refresh() {
        try {
            List<ServiceCategory> categories = masterData.getActiveCategories();
            Map<Long, String> codeById = new HashMap<>();
            List<LiveCatalog.Category> cats = new ArrayList<>();
            for (ServiceCategory c : categories) {
                codeById.put(c.getId(), c.getCode());
                cats.add(new LiveCatalog.Category(c.getCode(), c.getName(), c.getDescription()));
            }
            List<Item> items = new ArrayList<>();
            for (Service s : masterData.getServices(null)) {
                String group = codeById.get(s.getCategoryId());
                if (group == null || Boolean.FALSE.equals(s.getIsActive())) {
                    continue; // gói thuộc danh mục đã tắt
                }
                long price = s.getBasePrice() != null ? s.getBasePrice().longValue() : 0;
                items.add(LiveCatalog.fromDatabase(s.getCode(), group, s.getName(), s.getDescription(), price,
                        s.getEstimatedDurationMinutes()));
            }
            List<LiveCatalog.Area> areas = masterData.getActiveAreas().stream()
                    .map(a -> new LiveCatalog.Area(a.code(), a.name())).toList();
            LiveCatalog.update(new LiveCatalog.Snapshot(cats, items, areas, true));
        } catch (Exception ex) {
            LOG.warn("Chatbot: chưa đọc được dữ liệu dịch vụ từ database, giữ dữ liệu cũ: {}", ex.getMessage());
        }
    }
}
