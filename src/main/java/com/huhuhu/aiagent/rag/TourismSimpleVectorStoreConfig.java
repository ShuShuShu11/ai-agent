package com.huhuhu.aiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 浙江旅游助手 - SimpleVectorStore 配置
 *
 * 使用流程：加载文档 → 切分 → 关键词增强 → 向量存储
 */
@Configuration
public class TourismSimpleVectorStoreConfig {

    @Resource
    private TourismDocumentLoader tourismDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore tourismSimpleVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();

        // 1. 加载 Markdown 文档
        List<Document> documentList = tourismDocumentLoader.loadMarkdowns();

        // 2. 切分文档
        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documentList);

        // 3. 关键词元信息增强
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(splitDocuments);

        // 4. 添加到向量存储
        simpleVectorStore.add(enrichedDocuments);

        return simpleVectorStore;
    }
}