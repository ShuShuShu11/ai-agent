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

    public SummarizingChatMemory(FileBasedChatMemory delegate, ChatModel chatModel, int maxRecentMessages) {
        this.delegate = delegate;
        this.chatClient = ChatClient.builder(chatModel).build();
        this.maxRecentMessages = maxRecentMessages;
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
        }
        result.addAll(recentMessages);
        return result;
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }

    /**
     * 检查是否需要摘要，并执行摘要
     */
    private void checkAndSummarize(String conversationId) {
        List<Message> messages = delegate.get(conversationId);
        if (messages.size() <= maxRecentMessages) {
            return;
        }

        List<Message> oldMessages = messages.subList(0, messages.size() - maxRecentMessages);
        List<Message> recentMessages = messages.subList(messages.size() - maxRecentMessages, messages.size());

        String summary = summarizeMessages(oldMessages);
        delegate.saveSummary(conversationId, summary);

        delegate.clear(conversationId);
        delegate.add(conversationId, recentMessages);

        log.info("对话 {} 已摘要，早期 {} 条消息压缩为摘要，保留 {} 条近期消息",
                conversationId, oldMessages.size(), maxRecentMessages);
    }

    /**
     * 调用 LLM 将旧消息压缩为摘要
     */
    private String summarizeMessages(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是对话历史的摘要：\n");

        for (Message msg : messages) {
            sb.append(msg.getMessageType()).append(": ").append(msg.getText()).append("\n");
        }

        String prompt = "请将以下对话内容压缩成一段简短的摘要，保留关键信息（用户意图、重要事实、已完成的任务）：\n\n" + sb.toString();

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
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