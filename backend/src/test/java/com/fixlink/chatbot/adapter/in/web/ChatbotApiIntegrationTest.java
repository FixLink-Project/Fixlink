package com.fixlink.chatbot.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatbotApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Lấy gợi ý câu hỏi theo trang (suggestions) thành công")
    void shouldReturnSuggestionsForHomeRoute() throws Exception {
        mockMvc.perform(get("/api/v1/chatbot/suggestions")
                        .param("route", "/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("Gửi tin nhắn tới chatbot và nhận phản hồi thành công")
    void shouldHandleMessageSuccessfully() throws Exception {
        Map<String, Object> req = Map.of(
                "sessionId", "test-session-integration-1",
                "message", "FixLink hoạt động như thế nào?",
                "pageContext", Map.of("route", "/")
        );

        mockMvc.perform(post("/api/v1/chatbot/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.reply").isString())
                .andExpect(jsonPath("$.data.sessionId").value("test-session-integration-1"));
    }

    @Test
    @DisplayName("Gửi phản hồi đánh giá (thumbs up/down) thành công")
    void shouldRecordFeedbackSuccessfully() throws Exception {
        Map<String, Object> req = Map.of(
                "sessionId", "test-session-integration-1",
                "messageId", "msg-123",
                "rating", "UP",
                "comment", "Rất hữu ích"
        );

        mockMvc.perform(post("/api/v1/chatbot/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @DisplayName("Xóa phiên hội thoại (reset session) thành công")
    void shouldDeleteSessionSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/chatbot/sessions/test-session-integration-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));
    }
}
