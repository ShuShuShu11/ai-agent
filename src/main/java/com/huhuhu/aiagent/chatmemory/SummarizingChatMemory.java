package com.huhuhu.aiagent.chatmemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;

import org.springframework.ai.content.Content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 带自动摘要功能的 ChatMemory
 * <p>当对话历史超过阈值时，自动将早期消息压缩为摘要，
 * 只保留摘要 + 近期原始消息，减少 token 消耗同时保留关键信息
 */
@Slf4j
public class SummarizingChatMemory implements ChatMemory {

    private final FileBasedChatMemory delegate;
    private final ChatClient chatClient;
    private final int maxRecentMessages;
    private final int summarizeThreshold;

    public SummarizingChatMemory(FileBasedChatMemory delegate, ChatModel chatModel, int maxRecentMessages) {
        this(delegate, chatModel, maxRecentMessages, maxRecentMessages * 2);
    }

    public SummarizingChatMemory(FileBasedChatMemory delegate, ChatModel chatModel, int maxRecentMessages, int summarizeThreshold) {
        this.delegate = delegate;
        this.chatClient = ChatClient.builder(chatModel).build();
        this.maxRecentMessages = maxRecentMessages;
        this.summarizeThreshold = summarizeThreshold;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        delegate.add(conversationId, messages);
        checkAndSummarize(conversationId);
    }

    @Override
    public List<Message> get(String conversationId) {
        List<Message> recentMessages = delegate.get(conversationId);
        String summary = delegate.getSummary(conversationId);

        List<Message> result = new ArrayList<>();
        if (summary != null && !summary.isEmpty()) {
            result.add(new SummarizedMemoryMessage(summary));
            log.debug("对话 {} 返回摘要，长度 {} 字符 + {} 条近期消息",
                    conversationId, summary.length(), recentMessages.size());
        }
        result.addAll(recentMessages);
        return result;
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }

    /**
     * 检查是否需要摘要，只有超过 summarizeThreshold（默认 maxRecentMessages * 2）时才触发
     * 摘要后保留 maxRecentMessages 条近期消息，下次添加后不会频繁触发摘要
     */
    private void checkAndSummarize(String conversationId) {
        List<Message> messages = delegate.get(conversationId);
        // 只有超过阈值才摘要，避免每次都触发
        if (messages.size() <= summarizeThreshold) {
            return;
        }

        String existingSummary = delegate.getSummary(conversationId);
        String newSummary;
        if (existingSummary != null && !existingSummary.isEmpty()) {
            // 已有摘要，将旧消息 + 旧摘要 合并再压缩
            List<Message> combinedMessages = new ArrayList<>();
            combinedMessages.add(new SummarizedMemoryMessage(existingSummary));
            combinedMessages.addAll(messages);
            newSummary = summarizeMessages(combinedMessages);
        } else {
            newSummary = summarizeMessages(messages);
        }

        // 摘要后只保留近期消息
        List<Message> recentMessages = messages.subList(messages.size() - maxRecentMessages, messages.size());
        delegate.saveSummary(conversationId, newSummary);
        delegate.clear(conversationId);
        delegate.add(conversationId, recentMessages);

        log.info("对话 {} 摘要完成，输入 {} 条消息 → 摘要长度 {} 字符，保留 {} 条近期消息",
                conversationId, messages.size(), newSummary.length(), maxRecentMessages);
    }

    /**
     * 调用 LLM 将旧消息压缩为摘要
     */
    private String summarizeMessages(List<Message> messages) {
        log.debug("开始摘要，共 {} 条消息", messages.size());

        StringBuilder sb = new StringBuilder();
        sb.append("以下是对话历史的摘要：\n");

        for (Message msg : messages) {
            sb.append(msg.getMessageType()).append(": ").append(msg.getText()).append("\n");
        }

        String prompt = "请将以下对话内容压缩成一段简短的摘要，保留关键信息（用户意图、重要事实、已完成的任务）：\n\n" + sb.toString();

        String summary = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.debug("摘要生成完成，长度 {} 字符", summary != null ? summary.length() : 0);
        return summary;
    }

    /**
     * 表示摘要的消息类型
     */
    public static class SummarizedMemoryMessage implements Message {
        private final String content;

        public SummarizedMemoryMessage(String content) {
            this.content = content;
        }

        @Override
        public String getText() {
            return content;
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.SYSTEM;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Map.of("summary", true);
        }
    }
}