# AI Agent 智能体项目

## 项目介绍

这是一个 AI 智能体项目，包含：
- **ai-agent**: Spring Boot 3 后端（AI 恋爱大师 + 智能体）
- **ai-agent-frontend**: Vue 3 前端
- **image-search-mcp-server**: MCP 图片搜索服务

AI 恋爱大师应用可以依赖 AI 大模型解决用户的情感问题，支持多轮对话、基于自定义知识库进行问答、自主调用工具完成任务。

智能体基于 ReAct 模式，可以利用网页搜索、资源下载和 PDF 生成工具，帮用户制定完整的约会计划并生成文档。

## 技术栈

- Java 17 + Spring Boot 3
- Spring AI + LangChain4j
- RAG 知识库
- PGvector 向量数据库
- Tool Calling 工具调用
- MCP 模型上下文协议
- ReAct Agent 智能体构建
- 阿里云百练 DashScope

## 快速开始

### 后端

```bash
./mvnw spring-boot:run
```

### 前端

```bash
cd ai-agent-frontend
npm install
npm run dev
```

### MCP 服务

```bash
cd image-search-mcp-server
./mvnw spring-boot:run
```
