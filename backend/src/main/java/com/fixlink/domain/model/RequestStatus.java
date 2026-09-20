package com.fixlink.domain.model;

/**
 * 10 trạng thái cốt lõi của một yêu cầu sửa chữa theo tài liệu thiết kế FixLink.
 */
public enum RequestStatus {
    PENDING,                    // Mới tạo chờ duyệt hoặc mở thầu
    DRAFT,                      // Bản nháp khách lưu chưa đăng
    BIDDING_OPEN,               // Mở nhận báo giá / chào thầu
    MATCHED_AWAITING_DEPOSIT,   // Đã chọn thợ, chờ khách cọc 30% Escrow
    ASSIGNED,                   // Đã cọc thành công, thợ chính thức nhận việc
    INSPECTING,                 // Thợ đến khảo sát hiện trường & lập biên bản
    AWAITING_COST_APPROVAL,     // Phát hiện lỗi thêm, chờ khách duyệt chi phí phát sinh
    IN_PROGRESS,                // Thợ đang thực hiện sửa chữa
    AWAITING_ACCEPTANCE,        // Thợ báo hoàn thành, chờ nghiệm thu & tất toán 70%
    COMPLETED,                  // Khách đã nghiệm thu, giải ngân ví thợ, xuất bảo hành
    CANCELLED                   // Đơn bị hủy (hết hạn thầu, hủy trước sửa, từ chối phát sinh)
}
