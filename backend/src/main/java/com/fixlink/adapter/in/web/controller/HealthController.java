package com.fixlink.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Điểm kiểm tra sức khoẻ cho container và pipeline triển khai.
 *
 * <p>Trả 200 ngay khi ứng dụng nhận được request, không đụng tới cơ sở dữ liệu
 * để healthcheck vẫn phản hồi nhanh và không phụ thuộc dữ liệu mẫu.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Kiểm tra sức khoẻ hệ thống")
public class HealthController {

    @GetMapping
    @Operation(summary = "Kiểm tra ứng dụng còn sống")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
