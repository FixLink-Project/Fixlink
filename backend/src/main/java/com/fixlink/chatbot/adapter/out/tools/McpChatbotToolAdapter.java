package com.fixlink.chatbot.adapter.out.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class McpChatbotToolAdapter implements ChatbotToolPort {

    private static final Logger LOG = LoggerFactory.getLogger(McpChatbotToolAdapter.class);
    private static final long READ_TIMEOUT_SECONDS = 30;

    private final NativeChatbotToolAdapter nativeAdapter;
    private final ChatbotProperties properties;
    private final ObjectMapper objectMapper;

    /** Reusable MCP subprocess — started once and kept alive across calls. */
    private volatile Process mcpProcess;
    private volatile BufferedWriter mcpWriter;
    private volatile BufferedReader mcpReader;
    private final ReentrantLock processLock = new ReentrantLock();
    private final AtomicInteger requestIdCounter = new AtomicInteger(10);

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
                destroyMcpProcess(); // Reset the process on failure so next call gets a fresh one
                return nativeAdapter.executeTool(toolName, arguments, currentUserId, currentRole, guestContact);
            }
        }

        // Domain specific FixLink tools execute via native adapter
        return nativeAdapter.executeTool(toolName, arguments, currentUserId, currentRole, guestContact);
    }

    /**
     * Resolves the MCP server.js path from config (chatbot.mcp.server-path),
     * with fallback to a list of conventional relative paths.
     */
    private File resolveServerJsPath() {
        String configuredPath = properties.getTools().getMcp().getServerPath();

        // 1. Try the explicitly configured path first
        if (configuredPath != null && !configuredPath.isBlank()) {
            File f = new File(configuredPath);
            if (f.exists()) {
                return f;
            }
            LOG.warn("Configured MCP server path '{}' does not exist. Trying fallback paths...", configuredPath);
        }

        // 2. Fallback: conventional relative paths
        for (String candidate : List.of(
                "../MCPSERVER/server.js",
                "../../MCPSERVER/server.js",
                "MCPSERVER/server.js"
        )) {
            File f = new File(candidate);
            if (f.exists()) {
                return f;
            }
        }

        throw new IllegalStateException(
                "Cannot find MCPSERVER/server.js. Configure 'chatbot.mcp.server-path' in application.yml or place server.js in a conventional location."
        );
    }

    /**
     * Starts or returns the persistent MCP subprocess.
     * The process is initialized once and reused for all subsequent calls.
     */
    private void ensureMcpProcessStarted() throws Exception {
        if (mcpProcess != null && mcpProcess.isAlive()) {
            return;
        }

        processLock.lock();
        try {
            // Double-check after acquiring lock
            if (mcpProcess != null && mcpProcess.isAlive()) {
                return;
            }

            File serverJs = resolveServerJsPath();
            String command = properties.getTools().getMcp().getCommand();

            ProcessBuilder pb = new ProcessBuilder(command, serverJs.getAbsolutePath());
            pb.redirectErrorStream(false);
            mcpProcess = pb.start();

            mcpWriter = new BufferedWriter(new OutputStreamWriter(mcpProcess.getOutputStream(), StandardCharsets.UTF_8));
            mcpReader = new BufferedReader(new InputStreamReader(mcpProcess.getInputStream(), StandardCharsets.UTF_8));

            // Initialize the MCP protocol
            String initReq = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"clientInfo\":{\"name\":\"fixlink-chatbot-client\",\"version\":\"1.0.0\"},\"capabilities\":{}}}\n";
            mcpWriter.write(initReq);
            mcpWriter.flush();

            String initRes = readLineWithTimeout(mcpReader);
            LOG.debug("MCP Init response: {}", initRes);

            // Initialized notification
            mcpWriter.write("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}\n");
            mcpWriter.flush();

            LOG.info("MCP subprocess started and initialized (PID: {}, server: {})", mcpProcess.pid(), serverJs.getAbsolutePath());
        } finally {
            processLock.unlock();
        }
    }

    private ToolExecutionResult callMcpServer(
            String toolName,
            Map<String, Object> arguments,
            Long currentUserId,
            String currentRole,
            String guestContact
    ) throws Exception {
        ensureMcpProcessStarted();

        processLock.lock();
        try {
            int reqId = requestIdCounter.incrementAndGet();

            // Map tool name to MCP tool
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
                    "id", reqId,
                    "method", "tools/call",
                    "params", Map.of(
                            "name", mcpToolName,
                            "arguments", mcpArgs
                    )
            );

            mcpWriter.write(objectMapper.writeValueAsString(callReq) + "\n");
            mcpWriter.flush();

            String callRes = readLineWithTimeout(mcpReader);
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
            processLock.unlock();
        }
    }

    /**
     * Reads a line from the MCP subprocess with a timeout.
     * If the read exceeds READ_TIMEOUT_SECONDS (30s), the process is killed and an error is thrown.
     */
    private String readLineWithTimeout(BufferedReader reader) throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                return reader.readLine();
            } catch (Exception e) {
                throw new RuntimeException("Error reading from MCP subprocess", e);
            }
        });

        try {
            return future.get(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOG.error("MCP subprocess read timed out after {} seconds. Killing process...", READ_TIMEOUT_SECONDS);
            destroyMcpProcess();
            throw new IllegalStateException("MCP Server read timeout after " + READ_TIMEOUT_SECONDS + " seconds");
        } catch (ExecutionException e) {
            throw new IllegalStateException("MCP Server read error: " + e.getCause().getMessage(), e.getCause());
        }
    }

    /**
     * Destroys the persistent MCP subprocess and resets state.
     */
    private void destroyMcpProcess() {
        processLock.lock();
        try {
            if (mcpProcess != null) {
                try {
                    if (mcpWriter != null) {
                        mcpWriter.close();
                    }
                } catch (Exception ignored) {
                }
                try {
                    if (mcpReader != null) {
                        mcpReader.close();
                    }
                } catch (Exception ignored) {
                }
                mcpProcess.destroyForcibly();
                try {
                    mcpProcess.waitFor(2, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                mcpProcess = null;
                mcpWriter = null;
                mcpReader = null;
                LOG.info("MCP subprocess destroyed");
            }
        } finally {
            processLock.unlock();
        }
    }

    @PreDestroy
    public void shutdown() {
        destroyMcpProcess();
    }
}
