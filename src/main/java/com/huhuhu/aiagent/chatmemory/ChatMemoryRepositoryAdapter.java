package com.huhuhu.aiagent.chatmemory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * ChatMemory 到 ChatMemoryRepository 的适配器
 * <p>将 ChatMemory 适配为 ChatMemoryRepository，使 MessageWindowChatMemory 可以使用
 *
 * @deprecated 已废弃。现在 SummarizingChatMemory 直接实现 ChatMemory 接口，
 *             不再需要通过适配器转换。保留此类以备将来需要兼容旧代码时使用。
 */
public class ChatMemoryRepositoryAdapter implements ChatMemoryRepository {

    private final ChatMemory chatMemory;

    public ChatMemoryRepositoryAdapter(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @Override
    public List<String> findConversationIds() {
        // FileBasedChatMemory 不支持查询所有会话 ID，返回空列表
        return List.of();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return chatMemory.get(conversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        chatMemory.add(conversationId, messages);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        chatMemory.clear(conversationId);
    }
}