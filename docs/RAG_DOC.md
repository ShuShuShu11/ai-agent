# RAG 知识库实现文档

## 一、整体架构

```
用户输入
    │
    ▼
QueryRewriter.doQueryRewrite()     ← 查询改写（非必须）
    │
    ▼
shouldUseRag() 判断                 ← 判断是否走RAG
    │  走 RAG: 旅游相关问题
    │  不走 RAG: 闲聊/个人信息
    ▼
TourismRagCustomAdvisorFactory
    │  创建 RetrievalAugmentationAdvisor
    ▼
VectorStoreDocumentRetriever
    │  similarityThreshold=0.5, topK=3
    ▼
向量数据库（SimpleVectorStore / PgVector）
    │
    ▼
LLM (qwen-plus) 生成回答
```

## 二、向量数据库

| 向量库 | 配置类 | 状态 |
|--------|--------|------|
| **SimpleVectorStore** | `TourismSimpleVectorStoreConfig` | ✅ 默认启用 |
| **PgVector** | `TourismPgVectorStoreConfig` | ❌ 需配置数据库 |

### SimpleVectorStore（内存，默认）

```java
// 启动时自动加载文档到内存
VectorStore tourismSimpleVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
    SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
    // 加载 document/杭州/*.md
}
```

### PgVector（可选）

启用条件：`application.yml` 配置了 `spring.datasource.url`

```java
@Configuration
@ConditionalOnProperty(name = "spring.datasource.url")
public class TourismPgVectorStoreConfig { }
```

## 三、文档结构（按城市维度）

```
document/
├── 杭州/              ← 当前只加载杭州
│   ├── 景点.md        → 元信息: city=杭州, type=景点
│   ├── 美食.md        → 元信息: city=杭州, type=美食
│   ├── 交通.md        → 元信息: city=杭州, type=交通
│   └── 路线.md        → 元信息: city=杭州, type=路线
├── 宁波/
├── 温州/
├── ...（其他8市）
└── 丽水/
```

**加载配置：**
```java
// TourismDocumentLoader.java
private static final String[] CITIES = {
    "杭州"  // 只加载杭州，加速启动
};
```

添加新城市：修改 `CITIES` 数组

## 四、文档处理流程

```
document/杭州/景点.md
        │
        ▼
TourismDocumentLoader.loadMarkdowns()
        │  遍历 CITIES，按城市加载
        ▼
MyTokenTextSplitter.splitCustomized()
        │  200 tokens 块大小，100 overlap
        ▼
MyKeywordEnricher.enrichDocuments()
        │  AI 补充关键词元信息
        ▼
SimpleVectorStore.add()
        │  写入内存向量存储
        ▼
用户查询时：
        ▼
VectorStoreDocumentRetriever
        │  similarityThreshold=0.5, topK=3
        ▼
LLM 生成回答
```

## 五、核心组件

| 组件 | 文件 | 作用 |
|------|------|------|
| 文档加载 | `TourismDocumentLoader.java` | 按城市加载 `document/{城市}/*.md` |
| 文档切分 | `MyTokenTextSplitter.java` | Token-based 切分 |
| 关键词增强 | `MyKeywordEnricher.java` | AI 补充关键词 |
| 向量存储 | `TourismSimpleVectorStoreConfig.java` | SimpleVectorStore |
| 向量存储 | `TourismPgVectorStoreConfig.java` | PgVector（条件加载） |
| RAG 工厂 | `TourismRagCustomAdvisorFactory.java` | 创建 RAG Advisor |
| 查询改写 | `QueryRewriter.java` | 改写用户查询 |

## 六、RAG 工厂方法

```java
// 创建 RAG Advisor（可按城市过滤）
TourismRagCustomAdvisorFactory.createTourismRagCustomAdvisor(vectorStore, "杭州")

// 参数说明：
// - vectorStore: 向量存储
// - city: 城市过滤（null/空则不过滤）
```

## 七、接口

| 接口 | 功能 |
|------|------|
| `GET /ai/tourism/chat/with_tools_and_rag/sse` | 工具+RAG（主要使用） |
| `GET /ai/tourism/chat/sse` | 基础对话（无RAG） |

```bash
# 工具+RAG
curl "http://localhost:8123/api/ai/tourism/chat/with_tools_and_rag/sse?message=西湖门票多少钱&chatId=test"
```