# Issue Log

## 2026-05-27

### Issue: 会话记忆持久化 + 自动摘要

**功能描述：**
将会话记忆从内存改为基于本地文件存储，并实现消息超限时的自动摘要功能。

**完成内容：**
- [x] 新增 `FileBasedChatMemory`：基于 Kryo 序列化的本地文件存储
  - 支持相对路径（项目目录）和绝对路径
  - 分离存储：会话消息(.kryo) + 摘要(.summary.kryo)
- [x] 新增 `SummarizingChatMemory`：自动摘要功能
  - 全量压缩策略：所有消息压缩为摘要 + 保留近期消息
  - 渐进式压缩：已有摘要时合并旧摘要 + 新消息一起压缩
  - 配置：maxRecentMessages=20, summarizeThreshold=50
- [x] 新增 `clearConversation()`：清除会话时保留摘要
- [x] `ChatMemoryRepositoryAdapter` 标记 @deprecated
- [x] 更新 `docs/CHAT_MEMORY_DOC.md`：技术方案文档
- [x] 更新 `.gitignore`：忽略 .kryo 文件

**相关文件：**
- `src/main/java/com/huhuhu/aiagent/chatmemory/FileBasedChatMemory.java`
- `src/main/java/com/huhuhu/aiagent/chatmemory/SummarizingChatMemory.java`
- `src/main/java/com/huhuhu/aiagent/chatmemory/ChatMemoryRepositoryAdapter.java`
- `src/main/java/com/huhuhu/aiagent/app/TourismApp.java`
- `docs/CHAT_MEMORY_DOC.md`
- `.gitignore`

**Commit:** `6aa6fcf` - feat: 实现会话记忆持久化 + 自动摘要功能