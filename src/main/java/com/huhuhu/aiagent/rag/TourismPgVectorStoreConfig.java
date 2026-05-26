package com.huhuhu.aiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * 浙江旅游助手 - PgVector 配置
 *
 * 使用流程：加载文档 → 切分 → 关键词增强 → 向量存储
 */
@Configuration
public class TourismPgVectorStoreConfig {

    @Resource
    private TourismDocumentLoader tourismDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore tourismPgVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("tourism_vector_store")
                .maxDocumentBatchSize(10000)
                .build();

        // 1. 加载 Markdown 文档
        List<Document> documentList = tourismDocumentLoader.loadMarkdowns();

        // 2. 切分文档
        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documentList);

        // 3. 关键词元信息增强
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(splitDocuments);

        // 4. 添加到向量存储
        vectorStore.add(enrichedDocuments);

        return vectorStore;
    }
}