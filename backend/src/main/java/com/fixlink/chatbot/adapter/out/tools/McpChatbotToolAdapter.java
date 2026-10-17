package com.fixlink.chatbot.adapter.out.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class McpChatbotToolAdapter implements ChatbotToolPort {

    private static final Logger LOG = LoggerFactory.getLogger(McpChatbotToolAdapter.class);

    private final NativeChatbotToolAdapter nativeAdapter;
    private final ChatbotProperties properties;
    private final ObjectMapper objectMapper;

    public McpChatbotToolAdapter(
            NativeChatbotToolAdapter nativeAdapter,
            ChatbotProperties properties,
            ObjectMapper objectMapper
    ) {
        this.nativeAdapter = nativeAdapter;
        this.properties = properties;
        this.objectMapper = objectMapper;
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
        // Tickets tools can be executed via MCP Server (MCPSERVER/server.js)
        if ("create_support_ticket".equals(toolName) || "read_support_ticket".equals(toolName)) {
            try {
                return callMcpServer(toolName, arguments, currentUserId, currentRole, guestContact);
            } catch (Exception ex) {
                LOG.warn("MCP Server execution failed for '{}': {}. Falling back to native adapter...", toolName, ex.getMessage());
                return nativeAdapter.executeTool(toolName, arguments, currentUserId, currentRole, guestContact);
            }
        }

        // Domain specific FixLink tools execute via native adapter
        return nativeAdapter.executeTool(toolName, arguments, currentUserId, currentRole, guestContact);
    }

    private ToolExecutionResult callMcpServer(
            String toolName,
            Map<String, Object> arguments,
            Long currentUserId,
            String currentRole,
            String guestContact
    ) throws Exception {
        String command = properties.getTools().getMcp().getCommand();
        List<String> argsList = properties.getTools().getMcp().getArgs();

        File serverJs = null;
        for (String candidate : List.of(
                "../MCPSERVER/server.js",
                "../../MCPSERVER/server.js",
                "d:/chatbotAI-fixlink/MCPSERVER/server.js",
                "MCPSERVER/server.js"
        )) {
            File f = new File(candidate);
            if (f.exists()) {
                serverJs = f;
                break;
            }
        }

        if (serverJs == null) {
            throw new IllegalStateException("Cannot find MCPSERVER/server.js");
        }

        ProcessBuilder pb = new ProcessBuilder(command, serverJs.getAbsolutePath());
        pb.redirectErrorStream(false);
        Process process = pb.start();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            // 1. Initialize
            String initReq = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"clientInfo\":{\"name\":\"fixlink-chatbot-client\",\"version\":\"1.0.0\"},\"capabilities\":{}}}\n";
            writer.write(initReq);
            writer.flush();

            String initRes = reader.readLine();
            LOG.debug("MCP Init response: {}", initRes);

            // 2. Initialized notification
            writer.write("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}\n");
            writer.flush();

            // 3. Map tool name to MCP tool
            String mcpToolName = "create_support_ticket".equals(toolName) ? "create_ticket" : "read_ticket";
            Map<String, Object> mcpArgs = new java.util.HashMap<>();
            if ("create_support_ticket".equals(toolName)) {
                mcpArgs.put("title", arguments.getOrDefault("title", "Yêu cầu hỗ trợ"));
                mcpArgs.put("description", arguments.getOrDefault("description", ""));
                mcpArgs.put("priority", arguments.getOrDefault("priority", "Medium"));
                mcpArgs.put("assignee", "Chưa phân công");
            } else {
                if (arguments.containsKey("ticket_code")) {
                    mcpArgs.put("ticket_id", arguments.get("ticket_code"));
                }
            }

            Map<String, Object> callReq = Map.of(
                    "jsonrpc", "2.0",
                    "id", 2,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", mcpToolName,
                            "arguments", mcpArgs
                    )
            );

            writer.write(objectMapper.writeValueAsString(callReq) + "\n");
            writer.flush();

            String callRes = reader.readLine();
            if (callRes == null) {
                throw new IllegalStateException("MCP Server returned null response");
            }

            JsonNode node = objectMapper.readTree(callRes);
            if (node.has("error")) {
                return new ToolExecutionResult(node.get("error").toString(), true);
            }

            JsonNode result = node.get("result");
            if (result != null && result.has("content")) {
                JsonNode contentArr = result.get("content");
                if (contentArr.isArray() && contentArr.size() > 0) {
                    String text = contentArr.get(0).get("text").asText();
                    return new ToolExecutionResult(objectMapper.writeValueAsString(Map.of(
                            "mcpResponse", text,
                            "mode", "mcp"
                    )), false);
                }
            }

            return new ToolExecutionResult(callRes, false);
        } finally {
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
        }
    }
}
