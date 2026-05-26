# ai-agent 技术文档

## 一、项目结构

```
ai-agent/
├── app/LoveApp.java           ← AI 恋爱大师
├── agent/
│   ├── Manus.java           ← 超级智能体
│   ├── ToolCallAgent.java    ← 工具调用代理
│   ├── ReActAgent.java       ← ReAct 模式抽象类
│   └── BaseAgent.java        ← Agent 基类
├── tools/                     ← 7 个工具
├── rag/                      ← RAG 知识库配置
└── controller/AiController.java  ← HTTP 接口
```

---

## 二、两大应用

| 应用 | 入口类 | 功能 |
|------|--------|------|
| **LoveApp** | `app/LoveApp.java` | 情感咨询对话、RAG 知识库、工具调用 |
| **Manus** | `agent/Manus.java` | 超级智能体，ReAct 模式自主规划 |

---

## 三、工具列表

| 工具 | 功能 |
|------|------|
| `WebSearchTool` | 联网搜索（SearchAPI） |
| `FileOperationTool` | 文件读写 |
| `WebScrapingTool` | 网页抓取 |
| `ResourceDownloadTool` | 资源下载 |
| `TerminalOperationTool` | 终端命令 |
| `PDFGenerationTool` | PDF 生成 |
| `TerminateTool` | 终止任务 |

---

## 四、接口列表

### 4.1 LoveApp 接口

| 接口 | 功能 | 工具调用 |
|------|------|----------|
| `GET /ai/love_app/chat/sync` | 同步对话（基础） | 无 |
| `GET /ai/love_app/chat/sse` | 流式对话（基础） | 无 |
| `GET /ai/love_app/chat/with_tools/sync` | 同步对话（带工具） | WebSearch 等 |
| `GET /ai/love_app/chat/with_tools/sse` | 流式对话（带工具） | WebSearch 等 |
| `GET /ai/love_app/chat/rag` | RAG 知识库对话 | 检索增强 |

### 4.2 Manus 接口

| 接口 | 功能 | 工具调用 |
|------|------|----------|
| `GET /ai/manus/chat` | 超级智能体（ReAct 模式） | 全部 7 个工具 |

---

## 五、curl 命令

```bash
# LoveApp 同步（无工具）
curl "http://localhost:8123/api/ai/love_app/chat/sync?message=你好&chatId=test"

# LoveApp 带工具搜索
curl "http://localhost:8123/api/ai/love_app/chat/with_tools/sync?message=搜索今天的天气&chatId=test"

# LoveApp RAG 知识库问答
curl "http://localhost:8123/api/ai/love_app/chat/rag?message=单身如何脱单&chatId=test"

# Manus
curl "http://localhost:8123/api/ai/manus/chat?message=搜索今天的天气" -H "Accept: text/event-stream"
```

---

## 六、Postman 使用

1. **Method**: `GET`
2. **URL**: `http://localhost:8123/api/ai/love_app/chat/sync?message=你好&chatId=test`
3. **Params**: `message` = `你好`, `chatId` = `test`
4. **流式接口**: Headers 加 `Accept` = `text/event-stream`

---

## 七、技术栈

| 组件 | 技术 |
|------|------|
| 框架 | Spring Boot 3.4.4 + Java 17 |
| AI | Spring AI 1.0.0 + DashScope (qwen-plus) |
| 工具调用 | Spring AI Tool Calling |
| 向量数据库 | PgVector（默认禁用） |
| API 文档 | Knife4j |

---

## 八、关键配置

```yaml
# application.yml
spring:
  ai:
    dashscope:
      api-key: xxx          # 阿里云百练
      chat:
        options:
          model: qwen-plus

server:
  port: 8123
  servlet:
    context-path: /api

search-api:
  api-key: xxx              # SearchAPI
```
