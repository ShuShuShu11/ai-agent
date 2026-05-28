package com.huhuhu.aiagent.controller;

import com.huhuhu.aiagent.agent.huhuManus;
import com.huhuhu.aiagent.app.TourismApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private TourismApp tourismApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    // ==================== 浙江旅游助手 ====================

    /**
     * 基础对话（无工具、无 RAG）
     */
    @GetMapping(value = "/tourism/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithTourismSSE(String message, String chatId) {
        return tourismApp.doChatByStream(message, chatId);
    }

    /**
     * 工具 + RAG
     */
    @GetMapping(value = "/tourism/chat/with_tools_and_rag/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithTourismWithToolsAndRagSSE(String message, String chatId) {
        return tourismApp.doChatWithToolsAndRagStream(message, chatId);
    }

    /**
     * MCP 工具（调用高德地图 MCP）
     */
    @GetMapping(value = "/tourism/chat/with_mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithMcpSSE(String message, String chatId) {
        return tourismApp.doChatWithMcpTools(message, chatId);
    }

    /**
     * 工具 + RAG + MCP
     */
    @GetMapping(value = "/tourism/chat/with_all/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithToolsAndRagAndMcpSSE(String message, String chatId) {
        return tourismApp.doChatWithToolsAndRagAndMcpStream(message, chatId);
    }

    // ==================== Manus 超级智能体 ====================

    /**
     * Manus 超级智能体（ReAct 模式，可自主规划并调用本地工具）
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        huhuManus yuManus = new huhuManus(allTools, dashscopeChatModel);
        return yuManus.runStream(message);
    }
}