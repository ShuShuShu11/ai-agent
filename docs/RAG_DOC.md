# RAG 知识库实现文档

## 一、整体架构

```
用户查询
    │
    ▼
┌─────────────────────────────────────────────┐
│            QueryRewriter（查询改写）          │
└─────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────┐
│         RAG 检索层（3选1，可组合）            │
├─────────────────────────────────────────────┤
│  ① QuestionAnswerAdvisor（简单检索）        │
│  ② TourismRagCustomAdvisorFactory（自定义）  │
│  ③ tourismRagCloudAdvisor（云知识库）        │
└─────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────┐
│          向量数据库层（3选1，可组合）           │
├─────────────────────────────────────────────┤
│  ① SimpleVectorStore（内存，默认启用）        │
│  ② PgVector（需启动 PgVector 数据库）         │
│  ③ DashScope 云知识库（阿里云服务）            │
└─────────────────────────────────────────────┘
    │
    ▼
         LLM 生成回答
```

## 二、向量数据库（3个）

| 向量库 | 配置类 | 状态 | 说明 |
|--------|--------|------|------|
| **SimpleVectorStore** | `TourismVectorStoreConfig` | ✅ 默认启用 | 内存存储，临时 |
| **PgVector** | `PgVectorVectorStoreConfig` | ❌ 需手动启用 | 需启动 PgVector |
| **DashScope 云知识库** | `TourismRagCloudAdvisorConfig` | ✅ 启用 | 阿里云知识库服务 |

### 2.1 SimpleVectorStore（内存）

```java
// 已启用，配置类：TourismVectorStoreConfig.java
VectorStore tourismVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
    SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder().build();
    simpleVectorStore.add(enrichedDocuments);  // 启动时加载
    return simpleVectorStore;
}
```

### 2.2 PgVector

```java
// PgVectorVectorStoreConfig.java（已注释，需取消注释启用）
@Configuration
public class PgVectorVectorStoreConfig {
    VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .build();
    }
}
```

**启用方式：** 取消 `@Configuration` 注释 + 配置 `application.yml` 中的 datasource

### 2.3 DashScope 云知识库

```java
// TourismRagCloudAdvisorConfig.java（已启用）
Advisor tourismRagCloudAdvisor() {
    DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
    DocumentRetriever retriever = new DashScopeDocumentRetriever(dashScopeApi,
        DashScopeDocumentRetrieverOptions.builder()
            .withIndexName("浙江旅游助手")  // 知识库名称
            .build());
    return RetrievalAugmentationAdvisor.builder()
        .documentRetriever(retriever)
        .build();
}
```

## 三、RAG 检索层（3个 Advisor）

| Advisor | 用途 | 配置类 |
|---------|------|--------|
| `QuestionAnswerAdvisor` | 简单检索 | 内置 |
| `TourismRagCustomAdvisorFactory` | 自定义（过滤+阈值） | 自己实现 |
| `tourismRagCloudAdvisor` | 云知识库检索 | `TourismRagCloudAdvisorConfig` |

### 3.1 简单检索

```java
// doChatWithRag() 默认使用
.advisors(new QuestionAnswerAdvisor(tourismVectorStore))
```

### 3.2 自定义检索（支持过滤状态）

```java
// 支持按状态过滤（景点/美食/行程）、相似度阈值、返回数量
TourismRagCustomAdvisorFactory.createTourismRagCustomAdvisor(
    vectorStore, "景点"  // 只检索"景点"相关文档
)
```

### 3.3 云知识库检索

```java
// 使用阿里云知识库服务
.advisors(tourismRagCloudAdvisor)
```

## 四、文档处理流程

```
document/*.md（3个文档）
        │
        ▼
TourismDocumentLoader.loadMarkdowns()
        │  加载 Markdown 文档，提取 status 元信息
        ▼
MyKeywordEnricher.enrichDocuments()
        │  补充关键词元信息
        ▼
DashScope Embedding Model
        │  生成向量
        ▼
VectorStore（SimpleVectorStore / PgVector）
        │  存储向量
        ▼
检索时：QueryRewriter.doQueryRewrite()
                │  改写查询
                ▼
        QuestionAnswerAdvisor
                │  检索相似文档
                ▼
            LLM 生成回答
```

## 五、核心组件

| 组件 | 文件 | 作用 |
|------|------|------|
| 文档加载 | `TourismDocumentLoader.java` | 加载 `document/*.md` |
| 元信息增强 | `MyKeywordEnricher.java` | 补充关键词 |
| 文档切分 | `MyTokenTextSplitter.java` | 自定义切分 |
| 向量存储 | `TourismVectorStoreConfig.java` | SimpleVectorStore |
| 向量存储 | `PgVectorVectorStoreConfig.java` | PgVector |
| 云知识库 | `TourismRagCloudAdvisorConfig.java` | DashScope 云 |
| 查询改写 | `QueryRewriter.java` | 改写用户查询 |
| 自定义检索 | `TourismRagCustomAdvisorFactory.java` | 过滤+阈值 |
| 上下文增强 | `TourismContextualQueryAugmenterFactory.java` | 上下文增强 |

## 六、使用组合示例

### 6.1 当前默认（SimpleVectorStore + QuestionAnswerAdvisor）

```java
.advisors(new QuestionAnswerAdvisor(tourismVectorStore))
```

### 6.2 自定义检索（SimpleVectorStore + 过滤）

```java
.advisors(TourismRagCustomAdvisorFactory.createTourismRagCustomAdvisor(
    tourismVectorStore, "景点"
))
```

### 6.3 云知识库

```java
.advisors(tourismRagCloudAdvisor)
```

### 6.4 组合使用（多个 Advisor）

```java
.advisors(new QuestionAnswerAdvisor(tourismVectorStore))
.advisors(tourismRagCloudAdvisor)  // 同时使用本地和云
```

## 七、接口

| 接口 | 功能 |
|------|------|
| `GET /ai/tourism/chat/rag` | RAG 知识库对话 |

```bash
curl "http://localhost:8123/api/ai/tourism/chat/rag?message=杭州有哪些景点推荐&chatId=test"
```

## 八、知识库文档

位置：`src/main/resources/document/`

| 文档 | 状态标签 |
|------|----------|
| `浙江热门景点推荐.md` | 景点 |
| `浙江特色美食攻略.md` | 美食 |
| `浙江行程规划指南.md` | 行程 |
