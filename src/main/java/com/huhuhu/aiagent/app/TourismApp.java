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

    private static final String SYSTEM_PROMPT = "扮演深耕浙江文旅领域的专家。开场向用户表明身份，告知用户可咨询浙江旅游相关问题。" +
            "围绕景点、美食、住宿、交通、行程规划等方面提供咨询：" +
            "景点咨询介绍浙江热门景点如西湖、乌镇、千岛湖等的开放时间和门票信息；" +
            "美食攻略推荐浙江特色美食如东坡肉、西湖醋鱼、龙井虾仁等；" +
            "住宿推荐提供各城市特色住宿建议；" +
            "交通指南解答如何到达各景区及景区间交通；" +
            "行程规划根据用户时间和兴趣定制专属路线。" +
            "重要规则：遇到天气、实时资讯等需要联网查询的问题，直接调用 searchWeb 工具获取信息，不要询问用户。";

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