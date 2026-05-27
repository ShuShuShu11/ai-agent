# ai-agent 启动指南

## 一、启动前配置

### 1. 必需配置

**修改 `src/main/resources/application.yml`：**

```yaml
spring:
  ai:
    dashscope:
      api-key: your-real-api-key    # 阿里云百练 API Key（必填）

search-api:
  api-key: your-real-api-key       # SearchAPI Key（必填）
```

> 获取地址：
> - 阿里云百练：https://bailian.console.aliyun.com/
> - SearchAPI：https://www.searchapi.cn/

### 2. 可选配置（启用时才需要）

| 功能 | 配置项 | 说明 |
|------|--------|------|
| 本地大模型 | `spring.ai.ollama` | Ollama 需运行在 localhost:11434 |
| 向量数据库 | `spring.datasource` + `spring.ai.vectorstore.pgvector` | 需启动 PgVector |
| MCP 客户端 | `spring.ai.mcp.client` | 需启动 MCP 服务 |

---

## 二、启动清单

### 1. 后端服务 (ai-agent) ✅ 核心服务

```bash
cd C:\Users\evil\Desktop\project\ai-agent
./mvnw spring-boot:run
```

**启动参数（可选）：**
```bash
# 跳过数据库自动配置（开发调试用，不影响 RAG 功能）
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
```

**验证启动：**
- 控制台看到 `Started AiAgentApplication` 表示成功
- 访问 http://localhost:8123/api/swagger-ui.html

### 2. 前端 (ai-agent-frontend) 可选

```bash
cd ai-agent-frontend
npm install
npm run dev
```

**端口：** http://localhost:5173

### 3. MCP 服务 (image-search-mcp-server) 可选

```bash
cd image-search-mcp-server
./mvnw spring-boot:run
```

**端口：** http://localhost:8080

**启用 MCP 客户端：** 取消 application.yml 中 `spring.ai.mcp.client` 的注释

---

## 三、最简启动流程（只需后端）

```bash
# 1. 配置 API Key
#    编辑 application.yml，填入真实 Key

# 2. 启动后端
cd C:\Users\evil\Desktop\project\ai-agent
./mvnw spring-boot:run

# 3. 验证
#    浏览器打开 http://localhost:8123/api/swagger-ui.html
```

---

## 四、VS Code 启动方式

1. 安装 **Extension Pack for Java**（微软官方）
2. 打开 `AiAgentApplication.java`
3. 右键 → **Run Java** 或 **Debug Java**

---

## 五、已知限制

- **数据库**：默认禁用 DataSource，启动时会有警告但不影响运行
- **PgVector**：需自行启动 PgVector 服务并配置连接信息后才能使用
- **MCP**：需启动独立的 MCP 服务，本项目使用 `image-search-mcp-server` 作为示例
- **RAG 文档**：当前只加载杭州，其他城市需修改 `TourismDocumentLoader.CITIES` 数组