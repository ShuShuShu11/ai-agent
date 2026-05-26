package com.huhuhu.aiagent.controller;

import com.huhuhu.aiagent.agent.huhuManus;
import com.huhuhu.aiagent.app.LoveApp;
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
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    // ==================== LoveApp 基础对话 ====================

    /**
     * 同步对话（无工具）
     */
    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.doChat(message, chatId);
    }

    /**
     * SSE 流式对话（无工具）- 返回纯文本流
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式对话（无工具）- 返回标准 ServerSentEvent 格式，带 data: 前缀
     */
    @GetMapping(value = "/love_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式对话（无工具）- 使用 SseEmitter，支持超时控制和错误处理
     */
    @GetMapping(value = "/love_app/chat/sse_emitter")
    public SseEmitter doChatWithLoveAppServerSseEmitter(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(180000L);
        loveApp.doChatByStream(message, chatId)
                .subscribe(
                        chunk -> {
                            try {
                                sseEmitter.send(chunk);
                            } catch (IOException e) {
                                sseEmitter.completeWithError(e);
                            }
                        },
                        sseEmitter::completeWithError,
                        sseEmitter::complete
                );
        return sseEmitter;
    }

    // ==================== LoveApp 带工具调用 ====================

    /**
     * 同步对话（带工具调用）
     */
    @GetMapping("/love_app/chat/with_tools/sync")
    public String doChatWithLoveAppWithToolsSync(String message, String chatId) {
        return loveApp.doChatWithTools(message, chatId);
    }

    /**
     * SSE 流式对话（带工具调用）
     */
    @GetMapping(value = "/love_app/chat/with_tools/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppWithToolsSSE(String message, String chatId) {
        return loveApp.doChatWithToolsStream(message, chatId);
    }

    // ==================== LoveApp RAG 知识库 ====================

    /**
     * RAG 知识库对话（基于向量数据库检索增强）
     */
    @GetMapping("/love_app/chat/rag")
    public String doChatWithLoveAppRag(String message, String chatId) {
        return loveApp.doChatWithRag(message, chatId);
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
