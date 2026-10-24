package com.fixlink.chatbot.adapter.out.tools;

import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.application.service.ToolCallRecorder;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CompositeChatbotToolAdapter implements ChatbotToolPort {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeChatbotToolAdapter.class);

    private final NativeChatbotToolAdapter nativeAdapter;
    private final McpChatbotToolAdapter mcpAdapter;
    private final ChatbotProperties properties;
    private final ToolCallRecorder recorder;

    public CompositeChatbotToolAdapter(
            NativeChatbotToolAdapter nativeAdapter,
            McpChatbotToolAdapter mcpAdapter,
            ChatbotProperties properties,
            ToolCallRecorder recorder
    ) {
        this.nativeAdapter = nativeAdapter;
        this.mcpAdapter = mcpAdapter;
        this.properties = properties;
        this.recorder = recorder;
    }

    @Override
    public List<Map<String, Object>> getToolDeclarations() {
        return nativeAdapter.getToolDeclarations();
    }

    @Override
    public ToolExecutionResult executeTool(
            String toolName,
            Map<String, Object> arguments,
            Long currentUserId,
            String currentRole,
            String guestContact
    ) {
        ToolExecutionResult result = dispatch(toolName, arguments, currentUserId, currentRole, guestContact);
        recorder.record(toolName, result.content(), result.isError());
        return result;
    }

    private ToolExecutionResult dispatch(
            String toolName,
            Map<String, Object> arguments,
            Long currentUserId,
            String currentRole,
            String guestContact
    ) {
        String mode = properties.getTools().getMode();
        if ("mcp".equalsIgnoreCase(mode)) {
            try {
                return mcpAdapter.executeTool(toolName, arguments, currentUserId, currentRole, guestContact);
            } catch (Exception ex) {
                LOG.warn("MCP execution failed in Composite adapter: {}. Falling back to native.", ex.getMessage());
                return nativeAdapter.executeTool(toolName, arguments, currentUserId, currentRole, guestContact);
            }
        }
        return nativeAdapter.executeTool(toolName, arguments, currentUserId, currentRole, guestContact);
    }
}
