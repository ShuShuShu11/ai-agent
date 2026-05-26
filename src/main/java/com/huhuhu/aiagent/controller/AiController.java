package com.huhuhu.aiagent.controller;

import com.huhuhu.aiagent.agent.huhuManus;
import com.huhuhu.aiagent.app.TourismApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private TourismApp tourismApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    // ==================== TourismApp 基础对话 ====================

    /**
     * 同步对话（无工具）
     */
    @GetMapping("/tourism/chat/sync")
    public String doChatWithTourismSync(String message, String chatId) {
        return tourismApp.doChat(message, chatId);
    }

    /**
     * SSE 流式对话（无工具）- 返回纯文本流
     */
    @GetMapping(value = "/tourism/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithTourismSSE(String message, String chatId) {
        return tourismApp.doChatByStream(message, chatId);
    }

    // ==================== TourismApp 带工具调用 ====================

    /**
     * 同步对话（带工具调用）
     */
    @GetMapping("/tourism/chat/with_tools/sync")
    public String doChatWithTourismWithToolsSync(String message, String chatId) {
        return tourismApp.doChatWithTools(message, chatId);
    }

    /**
     * SSE 流式对话（带工具调用）
     */
    @GetMapping(value = "/tourism/chat/with_tools/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithTourismWithToolsSSE(String message, String chatId) {
        return tourismApp.doChatWithToolsStream(message, chatId);
    }

    // ==================== TourismApp 工具+RAG 知识库 ====================

    /**
     * SSE 流式对话（带工具调用 + RAG 知识库）
     */
    @GetMapping(value = "/tourism/chat/with_tools_and_rag/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithTourismWithToolsAndRagSSE(String message, String chatId) {
        return tourismApp.doChatWithToolsAndRagStream(message, chatId);
    }

    // ==================== TourismApp RAG 知识库 ====================

    /**
     * RAG 知识库对话（基于向量数据库检索增强）
     */
    @GetMapping("/tourism/chat/rag")
    public String doChatWithTourismRag(String message, String chatId) {
        return tourismApp.doChatWithRag(message, chatId);
    }

    // ==================== huhuManus ====================

    /**
     * huhuManus 超级智能体（ReAct 模式，可自主规划并调用工具）
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        huhuManus yuManus = new huhuManus(allTools, dashscopeChatModel);
        return yuManus.runStream(message);
    }
}