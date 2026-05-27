# ai-agent 技术文档

## 一、项目结构

```
ai-agent/
├── app/
│   └── TourismApp.java              ← 浙江旅游助手（2个方法）
├── agent/
│   └── huhuManus.java              ← 超级智能体（ReAct模式）
├── controller/
│   └── AiController.java           ← HTTP 接口（3个端点）
├── rag/
│   ├── TourismDocumentLoader.java   ← 文档加载（按城市）
│   ├── TourismRagCustomAdvisorFactory.java ← RAG顾问工厂
│   ├── TourismSimpleVectorStoreConfig.java  ← 向量存储配置
│   ├── QueryRewriter.java           ← 查询改写
│   ├── MyTokenTextSplitter.java     ← 文档切分
│   ├── MyKeywordEnricher.java       ← 关键词增强
│   └── TourismPgVectorStoreConfig.java ← PgVector配置（条件加载）
├── tools/                           ← 工具实现
├── chatmemory/                      ← 对话记忆
└── docs/
    └── TECH_DOC.md                 ← 本文档
```

---

## 二、核心应用

### TourismApp（浙江旅游助手）

风格亲切随和，像朋友聊天一样。名称"呼呼"，是浙江旅游助手。

| 方法 | 功能 | RAG | 工具 |
|------|------|-----|------|
| `doChatByStream` | 基础对话 SSE | 否 | 无 |
| `doChatWithToolsAndRagStream` | 工具+RAG SSE（主要使用） | 是 | WebSearch 等 |

### huhuManus（超级智能体）

ReAct 模式自主规划，可调用全部工具完成任务。

---

## 三、接口列表

### 浙江旅游助手

| 接口 | 功能 |
|------|------|
| `GET /ai/tourism/chat/sse` | 基础对话（无工具无RAG） |
| `GET /ai/tourism/chat/with_tools_and_rag/sse` | 工具+RAG（主要使用） |

### Manus

| 接口 | 功能 |
|------|------|
| `GET /ai/manus/chat` | 超级智能体（SSE流） |

---

## 四、curl 命令

```bash
# 基础对话
curl "http://localhost:8123/api/ai/tourism/chat/sse?message=你好&chatId=test"

# 工具+RAG（主要）
curl "http://localhost:8123/api/ai/tourism/chat/with_tools_and_rag/sse?message=西湖门票多少钱&chatId=test"

# Manus
curl "http://localhost:8123/api/ai/manus/chat?message=帮我查下杭州天气" -H "Accept: text/event-stream"
```

---

## 五、RAG 知识库

### 文档结构（按城市维度）

```
document/
├── 杭州/          ← 当前只加载杭州
│   ├── 景点.md
│   ├── 美食.md
│   ├── 交通.md
│   └── 路线.md
├── 宁波/
├── 温州/
├── 嘉兴/
├── 湖州/
├── 绍兴/
├── 金华/
├── 衢州/
├── 舟山/
├── 台州/
└── 丽水/
```

- **共11个市**，当前只加载杭州（加速启动）
- 每个市4个文件：景点、美食、交通、路线
- 添加新城市：修改 `TourismDocumentLoader.CITIES` 数组

### RAG 流程

```
document/*.md
    ↓
TourismDocumentLoader.loadMarkdowns()
    ↓
MyTokenTextSplitter.splitCustomized()  ← 切分
    ↓
MyKeywordEnricher.enrichDocuments()   ← 关键词增强
    ↓
SimpleVectorStore.add()                ← 向量存储
    ↓
VectorStoreDocumentRetriever          ← 检索
    ↓
LLM (qwen-plus)                       ← 生成回答
```

---

## 六、工具列表

| 工具 | 功能 |
|------|------|
| `WebSearchTool` | 联网搜索（SearchAPI） |
| `FileOperationTool` | 文件读写 |
| `WebScrapingTool` | 网页抓取（Jsoup） |
| `ResourceDownloadTool` | 资源下载 |
| `TerminalOperationTool` | 终端命令 |
| `PDFGenerationTool` | PDF生成（iText） |
| `TerminateTool` | 终止任务 |

---

## 七、技术栈

| 组件 | 技术 |
|------|------|
| 框架 | Spring Boot 3.4.4 + Java 17 |
| AI | Spring AI 1.0.0 + DashScope (qwen-plus) |
| 工具调用 | Spring AI Tool Calling |
| 向量数据库 | SimpleVectorStore（默认）/ PgVector（可选） |
| 前端 | Vue 3 + Vite |
| API文档 | Knife4j |

---

## 八、关键配置

```yaml
# application.yml
spring:
  ai:
    dashscope:
      api-key: xxx              # 阿里云百练API Key
      chat:
        options:
          model: qwen-plus

server:
  port: 8123
  servlet:
    context-path: /api

search-api:
  api-key: xxx                  # SearchAPI Key
```

---

## 九、扩展指南

### 添加新城市到RAG知识库

1. 在 `document/{城市}/` 下创建4个文件
2. 修改 `TourismDocumentLoader.java` 中的 `CITIES` 数组：
```java
private static final String[] CITIES = {
    "杭州", "宁波", "温州"  // 添加新城市
};
```

### 启用PgVector

1. 配置数据库连接
2. 取消 `application.yml` 中 datasource 和 vectorstore.pgvector 的注释
3. `TourismPgVectorStoreConfig` 会自动加载（通过 `@ConditionalOnProperty`）

### 添加新工具

1. 在 `tools/` 包下创建新的 Tool 类
2. 在 `AiController` 或 `Manus` 中注册 ToolCallback