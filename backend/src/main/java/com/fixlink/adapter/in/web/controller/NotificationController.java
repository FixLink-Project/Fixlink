package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.NotificationResponse;
import com.fixlink.application.port.in.NotificationUseCase;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Các API thông báo vòng đời hệ thống")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo của tôi", description = "Lấy danh sách thông báo mới nhất của người dùng hiện tại.")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<NotificationResponse> list = notificationUseCase.getMyNotifications(currentUser.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", list));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Số thông báo chưa đọc", description = "Đếm số thông báo chưa đọc của người dùng hiện tại để hiển thị badge.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        long count = notificationUseCase.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy số thông báo chưa đọc thành công", Map.of("unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu thông báo đã đọc", description = "Đánh dấu một thông báo cụ thể là đã đọc.")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id
    ) {
        notificationUseCase.markAsRead(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu thông báo đã đọc", null));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc", description = "Đánh dấu toàn bộ thông báo của người dùng là đã đọc.")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        notificationUseCase.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả thông báo đã đọc", null));
    }
}
