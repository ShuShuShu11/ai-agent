package com.huhuhu.aiagent.app;

import com.huhuhu.aiagent.advisor.MyLoggerAdvisor;
import com.huhuhu.aiagent.rag.QueryRewriter;
import com.huhuhu.aiagent.rag.TourismRagCustomAdvisorFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class TourismApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "你叫呼呼，是浙江旅游助手，风格亲切随和，像朋友聊天一样。" +
            "开场简单打招呼，告诉用户你是浙江旅游助手呼呼，随时可以问你浙江旅游相关问题。" +
            "你可以聊的：景点介绍、美食推荐、住宿建议、交通指南、行程规划。" +
            "遇到天气、实时资讯等需要联网的问题，直接调用 searchWeb 工具。" +
            "遇到不熟悉的问题，直接说不知道，不要编造。" +
            "回答要简洁友好，不要太正式。";

    public TourismApp(ChatModel dashscopeChatModel) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    @Resource
    private VectorStore tourismSimpleVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    @Resource
    private ToolCallback[] allTools;

    /**
     * 基础对话（SSE 流）
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * 工具 + RAG 知识库（SSE 流式传输）- 前端主要使用
     */
    public Flux<String> doChatWithToolsAndRagStream(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        return chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .advisors(TourismRagCustomAdvisorFactory.createTourismRagCustomAdvisor(tourismSimpleVectorStore, null))
                .toolCallbacks(allTools)
                .stream()
                .content();
    }
}