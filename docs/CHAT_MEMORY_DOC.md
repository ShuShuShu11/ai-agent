# 对话记忆超限处理方案

## 问题背景

当前 `MessageWindowChatMemory` 使用固定窗口策略（maxMessages=20），超过限制后新消息会覆盖旧消息，导致早期上下文丢失。

## 常见方案对比

| 方案 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **Fixed Window** | 直接丢弃旧消息 | 简单 | 丢失重要上下文 |
| **Sliding Window** | 固定窗口滑动 | 控制成本 | 仍是随机丢弃 |
| **Summary Window** | 旧消息压缩为摘要 | 保留核心信息 ✅ | 摘要质量依赖LLM |
| **Token Budget** | 按token预算管理 | 最优成本 | 实现复杂 |

---

## 推荐方案：Summary Window

### 核心思路

将历史对话"压缩"成摘要，只保留最近N条原始消息 + 早期摘要：

```
┌─────────────────────────────────────────┐
│          完整对话历史（100条）            │
├──────────────┬──────────────────────────┤
│ 早期对话(80条) │   最近对话(20条)           │
│      ↓        │                           │
│  LLM压缩成摘要  │                           │
│      ↓        │                           │
│   [早期摘要]   │  [最近的20条原始消息]       │
└──────────────┴──────────────────────────┘
```

### 触发时机

- 消息数达到阈值（如20条/50条）
- 或者 token 数接近模型上下文窗口的80%

### 实现方式

1. **自动摘要**：当达到阈值时，调用LLM将历史对话压缩成一段摘要
2. **双重记忆**：摘要作为"长期记忆"，原始消息作为"短期记忆"
3. **检索时综合**：查找时同时搜索摘要和近期原始消息

---

## 实现架构

```
FileBasedChatMemory
    │
    ├── 原始消息存储（近期）
    │     └── ./chat-memory/{conversationId}.kryo
    │
    └── 摘要存储
          └── ./chat-memory/{conversationId}.summary.kryo

SummarizingChatMemory（包装 FileBasedChatMemory）
    │
    ├── add(): 添加消息，超过阈值触发摘要
    ├── get(): 返回 摘要 + 近期原始消息
    └── clear(): 清理对话
```

### 关键类设计

```java
public class SummarizingChatMemory implements ChatMemory {
    private final ChatMemory delegate;        // FileBasedChatMemory
    private final ChatModel chatModel;         // 用于生成摘要
    private final int originalMessagesLimit;   // 保留原始消息数量
    private final int summarizeThreshold;     // 触发摘要的阈值

    @Override
    public void add(String conversationId, List<Message> messages) {
        delegate.add(conversationId, messages);

        // 超过阈值时，触发摘要
        List<Message> all = delegate.get(conversationId);
        if (all.size() > summarizeThreshold) {
            String summary = summarize(all);
            // 保存摘要，清除旧消息，只保留近期
        }
    }
}
```

---

## 方案选型建议

| 场景 | 推荐方案 |
|------|----------|
| 简单应用 | 调大 maxMessages（50-100），本地文件存储不怕 |
| 生产级应用 | Summary Window，自动压缩历史保留关键信息 |
| 长对话场景 | Token Budget，按模型上下文动态管理 |

---

## 下一步

实现 `SummarizingChatMemory`，整合到 `TourismApp` 替换现有方案。