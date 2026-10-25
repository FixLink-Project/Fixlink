package com.fixlink.chatbot.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fixlink.chatbot.domain.model.ChatAction;
import com.fixlink.chatbot.domain.model.ChatCard;
import com.fixlink.chatbot.domain.model.SourceReference;
import java.util.List;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {
    private String reply;
    private List<String> suggestions;
    private List<ChatAction> actions;
    private List<SourceReference> sources;
    private boolean needsHuman;
    private String ticketId;
    private String sessionId;
    private String modelUsed;
    /** Thẻ thông tin trực quan dựng từ dữ liệu thật của hệ thống (đơn, lịch hẹn, bảng giá...). */
    private List<ChatCard> cards;
    /** true khi giao diện nên hiện lời mời gặp nhân viên CSKH. */
    private boolean offerHandoff;
}
