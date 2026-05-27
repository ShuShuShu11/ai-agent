package com.huhuhu.aiagent.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 创建 RAG 检索增强顾问的工厂
 *
 * <p>执行链路（按顺序）：
 * <ol>
 *   <li>Pre-Retrieval：RewriteQueryTransformer 改写 Query（更利于检索）</li>
 *   <li>Filter：城市过滤（filterExpression）</li>
 *   <li>Retrieval：向量检索，返回 topK=3，similarityThreshold=0.5</li>
 *   <li>Post-Retrieval：ContextualQueryAugmenter(allowEmptyContext=true) 允许空上下文回答</li>
 * </ol>
 */
public class TourismRagCustomAdvisorFactory {

    /**
     * 创建自定义的 RAG 检索增强顾问（集成查询重写 + 允许空上下文）
     *
     * @param vectorStore 向量存储
     * @param city       城市过滤（传 null 或空字符串则不过滤）
     * @param chatModel  用于 QueryTransformer 的 ChatModel
     * @return 自定义的 RAG 检索增强顾问
     */
    public static Advisor createTourismRagCustomAdvisor(VectorStore vectorStore, String city, ChatModel chatModel) {
        DocumentRetriever baseRetriever;

        // 构建向量检索器（带城市过滤）
        if (city != null && !city.isEmpty()) {
            Filter.Expression expression = new FilterExpressionBuilder()
                    .eq("city", city)
                    .build();
            baseRetriever = VectorStoreDocumentRetriever.builder()
                    .vectorStore(vectorStore)
                    .filterExpression(expression)
                    .similarityThreshold(0.5)
                    .topK(3)
                    .build();
        } else {
            baseRetriever = VectorStoreDocumentRetriever.builder()
                    .vectorStore(vectorStore)
                    .similarityThreshold(0.5)
                    .topK(3)
                    .build();
        }

        // 包装为带日志的检索器
        DocumentRetriever documentRetriever = new LoggingDocumentRetriever(baseRetriever);

        // 构建查询重写转换器（用于 RAG 检索，不是发给 LLM 的消息）
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);
        QueryTransformer queryRewriter = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        // 构建 RAG Advisor：
        // 1. queryTransformers：检索前改写查询（仅用于检索，不改变发给 LLM 的原始消息）
        // 2. queryAugmenter(allowEmptyContext=true)：允许空上下文，不注入拒绝提示
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(queryRewriter)
                .documentRetriever(documentRetriever)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }
}