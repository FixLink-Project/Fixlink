package com.fixlink.chatbot.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class ChatMessageRequest {

    private String sessionId;

    @NotBlank(message = "Tin nhắn không được để trống")
    @Size(max = 1000, message = "Tin nhắn tối đa 1000 ký tự")
    private String message;

    private PageContextDto pageContext;

    private String guestContact;
}
