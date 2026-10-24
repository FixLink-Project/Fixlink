package com.fixlink.chatbot.application.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Ghi lại các lần gọi tool trong một lượt hỏi để dựng thẻ thông tin từ dữ liệu thật. Vòng tool
 * calling của mọi adapter LLM chạy đồng bộ trên luồng xử lý request, nên dùng ThreadLocal là đủ.
 */
@Component
public class ToolCallRecorder {

    public record ToolTrace(String toolName, String resultJson, boolean error) { }

    private final ThreadLocal<List<ToolTrace>> traces = new ThreadLocal<>();

    public void start() {
        traces.set(new ArrayList<>());
    }

    public void record(String toolName, String resultJson, boolean error) {
        List<ToolTrace> list = traces.get();
        if (list != null) {
            list.add(new ToolTrace(toolName, resultJson, error));
        }
    }

    /** Kết thúc ghi và trả về danh sách tool đã gọi (rỗng nếu chưa start). */
    public List<ToolTrace> stop() {
        List<ToolTrace> list = traces.get();
        traces.remove();
        return list != null ? list : List.of();
    }
}
