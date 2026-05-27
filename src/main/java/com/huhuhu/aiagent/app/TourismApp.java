package com.huhuhu.aiagent.app;

import com.huhuhu.aiagent.advisor.MyLoggerAdvisor;
import com.huhuhu.aiagent.chatmemory.FileBasedChatMemory;
import com.huhuhu.aiagent.chatmemory.SummarizingChatMemory;
import com.huhuhu.aiagent.rag.TourismRagCustomAdvisorFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class TourismApp {

    private final ChatClient chatClient;
    private final ChatModel dashscopeChatModel;

    private static final String SYSTEM_PROMPT = "你叫呼呼，是浙江旅游助手，风格亲切随和，像朋友聊天一样。" +
            "开场简单打招呼，告诉用户你是浙江旅游助手呼呼，随时可以问你浙江旅游相关问题。" +
            "你可以聊的：景点介绍、美食推荐、住宿建议、交通指南、行程规划。" +
            "遇到天气、实时资讯等需要联网的问题，直接调用 searchWeb 工具。" +
            "遇到不熟悉的问题，直接说不知道，不要编造。" +
            "回答要简洁友好，不要太正式。";

    // 浙江省11个地级市
    private static final String[] ZHEJIANG_CITIES = {
            "杭州", "宁波", "温州", "嘉兴", "湖州",
            "绍兴", "金华", "衢州", "舟山", "台州", "丽水"
    };

    private static final Random RANDOM = new Random();

    public TourismApp(ChatModel dashscopeChatModel) {
        this.dashscopeChatModel = dashscopeChatModel;

        // 1、基于内存的会话记忆
        // MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
        //         .chatMemoryRepository(new InMemoryChatMemoryRepository())
        //         .maxMessages(20)
        //         .build();

        // 2、基于本地文件的会话记忆（支持自动摘要）
        // 参数：存储目录、最大保留近期消息数、触发摘要的消息数阈值
        FileBasedChatMemory fileBasedChatMemory = new FileBasedChatMemory("./chat-memory");
        log.info("会话记忆存储路径: {}", fileBasedChatMemory.getBaseDir());

        SummarizingChatMemory chatMemory = new SummarizingChatMemory(
                fileBasedChatMemory, dashscopeChatModel, 2, 5);

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
     * 从用户消息中检测城市名称
     *
     * @param message 用户消息
     * @return 城市名称，多个则随机选一个，未检测到返回 null
     */
    private String detectCity(String message) {
        List<String> matchedCities = new ArrayList<>();
        for (String city : ZHEJIANG_CITIES) {
            if (message.contains(city)) {
                matchedCities.add(city);
            }
        }
        if (matchedCities.isEmpty()) {
            return null;
        }
        return matchedCities.get(RANDOM.nextInt(matchedCities.size()));
    }

    /**
     * 工具 + RAG 知识库（SSE 流式传输）- 前端主要使用
     * <p>RAG 链路：原始消息 → RewriteQueryTransformer(仅用于检索) → 向量检索 → LLM 回答
     */
    public Flux<String> doChatWithToolsAndRagStream(String message, String chatId) {
        String city = detectCity(message);
        log.debug("检测到城市: {}", city);

        return chatClient
                .prompt()
                .user(message)  // 原始消息发给 LLM
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                // RAG 链路：自动根据消息内容过滤城市 + queryTransformer(检索前改写) + allowEmptyContext
                .advisors(TourismRagCustomAdvisorFactory.createTourismRagCustomAdvisor(
                        tourismSimpleVectorStore, city, dashscopeChatModel))
                // 应用 tools
                .toolCallbacks(allTools)
                .stream()
                .content();
    }
}