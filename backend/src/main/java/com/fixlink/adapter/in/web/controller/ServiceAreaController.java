package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.AreaSimpleResponse;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceAreaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/areas")
@RequiredArgsConstructor
@Tag(name = "Service Areas", description = "Các API khu vực phục vụ / địa bàn hoạt động (Master Data)")
public class ServiceAreaController {

    private final SpringDataServiceAreaRepository serviceAreaRepository;

    @GetMapping
    @Operation(summary = "Lấy danh sách địa bàn hoạt động", description = "Danh sách quận/huyện và thành phố mà FixLink đang hỗ trợ cung cấp dịch vụ.")
    public ResponseEntity<ApiResponse<List<AreaSimpleResponse>>> getActiveAreas() {
        List<AreaSimpleResponse> list = serviceAreaRepository.findAllByIsActiveTrue()
                .stream()
                .map(a -> new AreaSimpleResponse(a.getId(), a.getCode(), a.getName(), a.getCity()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách địa bàn thành công", list));
    }
}
