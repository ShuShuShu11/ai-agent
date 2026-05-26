# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个 AI 智能体项目，包含：
- **ai-agent**: Spring Boot 3 后端（浙江旅游助手 + 智能体）
- **ai-agent-frontend**: Vue 3 前端
- **image-search-mcp-server**: MCP 图片搜索服务

## 常用命令

### 后端 (ai-agent)

```bash
# 编译打包
./mvnw clean package -DskipTests

# 运行（需在 src/main/resources/application.yml 配置 API Key）
./mvnw spring-boot:run

# 跳过数据库自动配置运行（开发调试用）
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
```

### 前端 (ai-agent-frontend)

```bash
cd ai-agent-frontend
npm install
npm run dev
```

### MCP 服务 (image-search-mcp-server)

```bash
cd image-search-mcp-server
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

## 技术栈

| 组件 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.4.4 + Java 17 |
| AI 框架 | Spring AI + LangChain4j |
| AI 模型 | 阿里云百练 DashScope (Qwen) |
| 向量数据库 | PgVector（已禁用默认配置） |
| 工具调用 | Spring AI Tool Calling |
| MCP | Spring AI MCP Client/Server |
| 前端 | Vue 3 + Vite 4 |

## 架构设计

### 核心模块层次

```
controller/AiController          ← HTTP 接口层
    ↓
app/TourismApp                   ← AI 应用层（旅游助手）
    ↓
agent/ReActAgent                ← 智能体抽象层
    ├── ToolCallAgent           ← 工具调用代理
    │       ↓
    │   Manus                    ← 超级智能体实现
    ↓
tools/                          ← 工具实现（文件/搜索/下载/PDF等）
    ↓
chatmemory/                     ← 对话记忆
rag/                            ← RAG 知识库
```

### Agent 体系（ReAct 模式）

```
BaseAgent
    ├── step()                  ← 单步执行
    └── think() + act()         ← 子类实现

ReActAgent（抽象）
    ├── think()                 ← AI 决策
    └── act()                   ← 工具执行

ToolCallAgent
    └── 使用 ToolCallingManager 执行工具

Manus（Spring Component）
    └── 继承 ToolCallAgent，具备自主规划能力
```

### 工具体系

| 工具 | 功能 |
|------|------|
| FileOperationTool | 文件读写操作 |
| WebSearchTool | 联网搜索（SearchAPI） |
| WebScrapingTool | 网页抓取（Jsoup） |
| ResourceDownloadTool | 资源下载 |
| TerminalOperationTool | 终端操作 |
| PDFGenerationTool | PDF 生成（iText） |
| TerminateTool | 终止任务 |

### 核心接口

| 端点 | 功能 |
|------|------|
| `GET /api/ai/tourism/chat/sync` | 同步聊天 |
| `GET /api/ai/tourism/chat/sse` | SSE 流式聊天 |
| `GET /api/ai/manus/chat` | Manus 智能体聊天 |

## 配置说明

关键配置在 `src/main/resources/application.yml`:
- `spring.ai.dashscope.api-key`: 阿里云百练 API Key（必填）
- `spring.ai.ollama`: 本地 Ollama 配置
- `server.port`: 8123
- `server.servlet.context-path`: /api

## 数据库

默认禁用 DataSourceAutoConfiguration。启用 PgVector 需：
1. 启动 PgVector 数据库
2. 取消 application.yml 中 datasource 和 vectorstore.pgvector 配置的注释

## MCP 服务

默认禁用 MCP Client。启用需：
1. 启动 MCP 服务（如 image-search-mcp-server）
2. 取消 application.yml 中 spring.ai.mcp.client 配置的注释

## 前端 API 代理

前端开发时通过 Vite 代理访问后端 `/api` 路径，配置在 `ai-agent-frontend/vite.config.js`。
