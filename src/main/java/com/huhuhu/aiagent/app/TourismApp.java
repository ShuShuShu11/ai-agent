package com.huhuhu.aiagent.app;

import com.huhuhu.aiagent.advisor.MyLoggerAdvisor;
import com.huhuhu.aiagent.rag.QueryRewriter;
import com.huhuhu.aiagent.rag.TourismRagCustomAdvisorFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

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
            "行程规划根据用户时间和兴趣定制专属路线。";

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

    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    record TourismReport(String title, List<String> suggestions) {

    }

    public TourismReport doChatWithReport(String message, String chatId) {
        TourismReport tourismReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成旅游攻略报告，标题为{目的地}旅游攻略，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(TourismReport.class);
        log.info("tourismReport: {}", tourismReport);
        return tourismReport;
    }

    // AI 旅游知识库问答功能

    @Resource
    private VectorStore tourismVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 和 RAG 知识库进行对话
     */
    public String doChatWithRag(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .advisors(new QuestionAnswerAdvisor(tourismVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 旅游助手（支持调用工具）
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 旅游助手（支持调用工具，SSE 流式传输）
     */
    public Flux<String> doChatWithToolsStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .stream()
                .content();
    }

    /**
     * AI 旅游助手（支持调用工具 + RAG 知识库，SSE 流式传输）
     */
    public Flux<String> doChatWithToolsAndRagStream(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        return chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .advisors(new QuestionAnswerAdvisor(tourismVectorStore))
                .toolCallbacks(allTools)
                .stream()
                .content();
    }

    // AI 调用 MCP 服务

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 旅游助手（调用 MCP 服务）
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}