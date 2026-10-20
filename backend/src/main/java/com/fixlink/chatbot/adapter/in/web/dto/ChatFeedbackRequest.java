package com.fixlink.chatbot.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatFeedbackRequest {

    @NotBlank(message = "sessionId không được để trống")
    private String sessionId;

    private String messageId;

    @NotBlank(message = "rating không được để trống (UP hoặc DOWN)")
    private String rating;

    private String comment;
}
