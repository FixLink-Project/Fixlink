package com.fixlink.domain.model;

/**
 * Các trạng thái của một yêu cầu sửa chữa.
 *
 * <p>Mô hình nghiệp vụ hiện tại là <b>"ai nhận trước được trước"</b>
 * (first-come-first-served): khách đăng yêu cầu kèm ngân sách cố định, thợ bấm
 * "Nhận việc" là nhận luôn, không còn bước khách so sánh nhiều báo giá.
 *
 * <pre>
 * DRAFT → OPEN → ASSIGNED → INSPECTING → AWAITING_COST_APPROVAL
 *       → IN_PROGRESS → AWAITING_ACCEPTANCE → COMPLETED | CANCELLED
 * </pre>
 */
public enum RequestStatus {
    PENDING,                    // Mới tạo chờ duyệt
    DRAFT,                      // Bản nháp khách lưu chưa đăng
    OPEN,                       // Đã đăng, đang chờ thợ nhận việc (tối đa 3 ngày)

    /**
     * @deprecated Thuộc mô hình đấu giá ngược cũ (nhiều thợ gửi báo giá cạnh
     *     tranh). Không dùng cho yêu cầu mới — hãy dùng {@link #OPEN}. Giữ lại
     *     để các bản ghi cũ trong DB vẫn đọc/hiển thị được.
     */
    @Deprecated
    BIDDING_OPEN,

    /**
     * @deprecated Thuộc mô hình cũ (khách chọn thợ xong mới chờ cọc). Mô hình
     *     mới đi thẳng từ {@link #OPEN} sang {@link #ASSIGNED} khi thợ nhận
     *     việc. Giữ lại cho dữ liệu cũ.
     */
    @Deprecated
    MATCHED_AWAITING_DEPOSIT,

    ASSIGNED,                   // Thợ đã nhận việc
    INSPECTING,                 // Thợ đến khảo sát hiện trường & lập biên bản
    AWAITING_COST_APPROVAL,     // Phát hiện lỗi thêm, chờ khách duyệt chi phí phát sinh
    IN_PROGRESS,                // Thợ đang thực hiện sửa chữa
    AWAITING_ACCEPTANCE,        // Thợ báo hoàn thành, chờ nghiệm thu & tất toán 70%
    COMPLETED,                  // Khách đã nghiệm thu, giải ngân ví thợ, xuất bảo hành
    CANCELLED                   // Đơn bị hủy (hết hạn 3 ngày, hủy trước sửa, từ chối phát sinh)
}
