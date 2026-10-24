package com.fixlink.chatbot.application.service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lớp chặn cuối chống bịa thông tin: LLM đôi khi tự nghĩ ra số hotline, email, website hay tỷ lệ phần trăm
 * mà FixLink chưa công bố. Mọi thông tin dạng này không xuất hiện trong hội thoại của chính khách hàng
 * sẽ bị thay bằng câu nói rõ là chưa công bố.
 */
@Component
public class ReplyGroundingGuard {

    private static final Logger LOG = LoggerFactory.getLogger(ReplyGroundingGuard.class);

    /** Tỷ lệ có thật trong quy định FixLink: cọc 30%, tất toán 70%. */
    private static final Set<String> KNOWN_PERCENTAGES = Set.of("30", "70", "100");

    private static final Pattern PHONE = Pattern.compile(
            "(?<![\\d.])(?:\\+?84|0)(?:[\\s.-]?\\d){8,10}(?![\\d])|(?<!\\d)1[89]00(?:[\\s.-]?\\d){4,6}(?!\\d)");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    private static final Pattern WEBSITE = Pattern.compile(
            "(?i)\\b(?:https?://\\S+|www\\.\\S+|[a-z0-9-]+\\.(?:vn|com|net|org|io)(?:/\\S*)?)");
    private static final Pattern PERCENT = Pattern.compile("(?<![\\d.,])(\\d{1,3}(?:[.,]\\d+)?)\\s?%");

    /**
     * @param conversationText toàn bộ nội dung người dùng đã gửi trong phiên (tin hiện tại + lịch sử);
     *                         thông tin khách tự cung cấp (ví dụ SĐT của khách) được giữ nguyên
     */
    public String enforce(String reply, String conversationText) {
        if (reply == null || reply.isBlank()) {
            return reply;
        }
        String context = conversationText != null ? conversationText : "";
        String digitsInContext = context.replaceAll("\\D", " ");

        String result = replaceUnless(PHONE, reply, m -> {
            String digits = m.group().replaceAll("\\D", "");
            return digitsInContext.contains(digits) || context.replaceAll("\\D", "").contains(digits);
        }, "(FixLink chưa công bố số điện thoại liên hệ)");

        result = replaceUnless(EMAIL, result, m -> context.toLowerCase().contains(m.group().toLowerCase()),
                "(FixLink chưa công bố email liên hệ)");

        result = replaceUnless(WEBSITE, result, m -> context.toLowerCase().contains(m.group().toLowerCase()),
                "website FixLink");

        result = replaceUnless(PERCENT, result, m -> {
            String value = m.group(1).replace(',', '.');
            String integer = value.contains(".") ? value.substring(0, value.indexOf('.')) : value;
            return KNOWN_PERCENTAGES.contains(integer) || context.contains(m.group(1) + "%")
                    || context.contains(m.group(1) + " %");
        }, "(tỷ lệ này FixLink chưa công bố)");

        if (!result.equals(reply)) {
            LOG.warn("Grounding guard removed unpublished contact info / figures from chatbot reply");
        }
        return result;
    }

    private interface Allow {
        boolean test(Matcher m);
    }

    private static String replaceUnless(Pattern pattern, String text, Allow allow, String replacement) {
        Matcher m = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(allow.test(m) ? m.group() : replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
