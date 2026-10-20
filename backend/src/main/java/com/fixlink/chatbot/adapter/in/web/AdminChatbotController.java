package com.fixlink.chatbot.adapter.in.web;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.chatbot.adapter.out.rag.InMemoryRagIndex;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/chatbot")
@Tag(name = "Admin Chatbot Management", description = "Các API quản trị cấu hình và chỉ mục kho tri thức chatbot")
@SecurityRequirement(name = "bearerAuth")
public class AdminChatbotController {

    private final InMemoryRagIndex ragIndex;

    public AdminChatbotController(InMemoryRagIndex ragIndex) {
        this.ragIndex = ragIndex;
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Quét và lập lại chỉ mục kho tri thức", description = "Admin kích hoạt tải lại toàn bộ tài liệu markdown từ resources/chatbot/knowledge/.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindex() {
        ragIndex.reindex();
        int count = ragIndex.getIndexedCount();
        return ResponseEntity.ok(ApiResponse.success(
                "Lập lại chỉ mục kho tri thức thành công",
                Map.of("indexedCount", count)
        ));
    }
}
