package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.application.port.in.AdminStatisticsUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/statistics")
@Tag(name = "Admin Statistics", description = "Admin statistics")
public class AdminStatisticsController {
    private final AdminStatisticsUseCase statisticsService;

    @GetMapping("/area-technicians")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String, Object>>> getVerifiedOfflineTechnicians(
            @RequestParam Long areaId,
            @RequestParam(required = false) Long categoryId) {
        return ApiResponse.success(statisticsService.getVerifiedOfflineTechnicians(areaId, categoryId));
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getMonthlyStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @RequestParam(required = false) Long areaId) {
        return ApiResponse.success(statisticsService.getMonthlyStatistics(month, areaId));
    }
}
