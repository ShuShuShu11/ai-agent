package com.huhuhu.aiagent.rag;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 创建自定义的 RAG 检索增强顾问的工厂
 */
public class TourismRagCustomAdvisorFactory {

    /**
     * 创建自定义的 RAG 检索增强顾问
     *
     * @param vectorStore 向量存储
     * @param status      状态过滤（传 null 或空字符串则不过滤）
     * @return 自定义的 RAG 检索增强顾问
     */
    public static Advisor createTourismRagCustomAdvisor(VectorStore vectorStore, String status) {
        DocumentRetriever documentRetriever;

        if (status != null && !status.isEmpty()) {
            // 过滤特定状态的文档
            Filter.Expression expression = new FilterExpressionBuilder()
                    .eq("status", status)
                    .build();
            documentRetriever = VectorStoreDocumentRetriever.builder()
                    .vectorStore(vectorStore)
                    .filterExpression(expression)
                    .similarityThreshold(0.5)
                    .topK(3)
                    .build();
        } else {
            // 不过滤，返回所有文档
            documentRetriever = VectorStoreDocumentRetriever.builder()
                    .vectorStore(vectorStore)
                    .similarityThreshold(0.5)
                    .topK(3)
                    .build();
        }

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }
}