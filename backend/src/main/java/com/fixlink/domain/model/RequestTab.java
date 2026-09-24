package com.fixlink.domain.model;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Nhóm trạng thái dùng cho thanh chia tab (Tab Bar) trên giao diện danh sách yêu cầu sửa chữa.
 * Ánh xạ 11 {@link RequestStatus} chi tiết thành 5 tab nghiệp vụ mà người dùng nhìn thấy.
 */
public enum RequestTab {

    ALL("Tất cả", Set.of()),
    AWAITING_QUOTE("Chờ báo giá", Set.of(
            RequestStatus.PENDING,
            RequestStatus.DRAFT,
            RequestStatus.BIDDING_OPEN,
            RequestStatus.MATCHED_AWAITING_DEPOSIT
    )),
    IN_PROGRESS("Đang thực hiện", Set.of(
            RequestStatus.ASSIGNED,
            RequestStatus.INSPECTING,
            RequestStatus.AWAITING_COST_APPROVAL,
            RequestStatus.IN_PROGRESS,
            RequestStatus.AWAITING_ACCEPTANCE
    )),
    COMPLETED("Hoàn thành", Set.of(RequestStatus.COMPLETED)),
    CANCELLED("Đã hủy", Set.of(RequestStatus.CANCELLED));

    private final String label;
    private final Set<RequestStatus> statuses;

    RequestTab(String label, Set<RequestStatus> statuses) {
        this.label = label;
        this.statuses = statuses;
    }

    public String getLabel() {
        return label;
    }

    public Set<RequestStatus> getStatuses() {
        return statuses;
    }

    /** Tab "Tất cả" không lọc theo trạng thái. */
    public boolean isAll() {
        return statuses.isEmpty();
    }

    /** Kiểm tra một trạng thái chi tiết có thuộc tab này hay không. */
    public boolean matches(RequestStatus status) {
        return isAll() || (status != null && statuses.contains(status));
    }

    /**
     * Chuẩn hóa tham số {@code tab} từ query string.
     * Hỗ trợ 2 cách truyền: tên tab nghiệp vụ (ALL, AWAITING_QUOTE, IN_PROGRESS, COMPLETED, CANCELLED)
     * hoặc tên trạng thái chi tiết (PENDING, IN_PROGRESS, COMPLETED...). Giá trị không hợp lệ trả về {@link #ALL}.
     */
    public static RequestTab fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ALL;
        }

        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (RequestTab tab : values()) {
            if (tab.name().equals(normalized)) {
                return tab;
            }
        }

        try {
            RequestStatus status = RequestStatus.valueOf(normalized);
            for (RequestTab tab : values()) {
                if (!tab.isAll() && tab.statuses.contains(status)) {
                    return tab;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Giá trị tab không hợp lệ -> trả về tab "Tất cả"
        }

        return ALL;
    }

    /** Danh sách tab hiển thị trên thanh điều hướng theo đúng thứ tự giao diện. */
    public static List<RequestTab> uiTabs() {
        return List.of(ALL, AWAITING_QUOTE, IN_PROGRESS, COMPLETED, CANCELLED);
    }
}
