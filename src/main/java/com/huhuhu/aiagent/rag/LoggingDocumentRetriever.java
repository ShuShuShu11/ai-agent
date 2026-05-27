package com.huhuhu.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

import java.util.List;

/**
 * 包装 DocumentRetriever，记录检索结果日志
 * <p>用于调试 RAG 链路，查看：
 * <ul>
 *   <li>检索命中的文档数量</li>
 *   <li>命中的文档内容</li>
 *   <li>文档的元信息（city、type、filename）</li>
 * </ul>
 */
@Slf4j
public class LoggingDocumentRetriever implements DocumentRetriever {

    private final DocumentRetriever delegate;

    public LoggingDocumentRetriever(DocumentRetriever delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<Document> retrieve(Query query) {
        List<Document> documents = delegate.retrieve(query);
        logRetrievedDocuments(query, documents);
        return documents;
    }

    private void logRetrievedDocuments(Query query, List<Document> documents) {
        log.info("========== RAG 检索结果 ==========");
        log.info("查询: {}", query.text());
        log.info("命中文档数: {}", documents.size());
        log.info("===================================");

        if (documents.isEmpty()) {
            log.warn("⚠️ 未命中任何文档");
        } else {
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                log.info("--- 文档 {}/{} ---", i + 1, documents.size());
                log.info("元信息: {}", doc.getMetadata());
                log.info("内容: {}", truncate(doc.getText(), 500));
            }
        }
        log.info("===================================");
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...(截断)";
    }
}