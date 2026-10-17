package com.fixlink.chatbot.application.port.out;

import java.util.List;
import java.util.Map;

public interface ChatbotToolPort {

    /**
     * Lấy định nghĩa danh sách các công cụ (Tool Function Declarations) theo chuẩn Gemini API.
     */
    List<Map<String, Object>> getToolDeclarations();

    /**
     * Thực thi một công cụ theo tên và tham số.
     */
    ToolExecutionResult executeTool(
            String toolName,
            Map<String, Object> arguments,
            Long currentUserId,
            String currentRole,
            String guestContact
    );

    record ToolExecutionResult(String content, boolean isError) { }
}
