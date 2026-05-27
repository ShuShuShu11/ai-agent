package com.huhuhu.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 浙江旅游助手应用文档加载器
 * 支持按城市维度加载文档：document/{城市}/{类型}.md
 */
@Component
@Slf4j
public class TourismDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    // 当前启用的城市（只加载杭州，加速启动）
    // 如需加载其他城市，修改此数组或在 loadMarkdowns() 中切换
    private static final String[] CITIES = {
        "杭州"
    };

    public TourismDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载多篇 Markdown 文档（按城市维度，只加载当前启用的城市）
     * @return 文档列表
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();

        for (String city : CITIES) {
            try {
                Resource[] resources = resourcePatternResolver.getResources("classpath:document/" + city + "/*.md");
                for (Resource resource : resources) {
                    String filename = resource.getFilename();
                    if (filename == null) continue;

                    // 提取文档类型（景点、美食、交通、路线）
                    String docType = filename.replace(".md", "");

                    MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                            .withHorizontalRuleCreateDocument(true)
                            .withIncludeCodeBlock(false)
                            .withIncludeBlockquote(false)
                            .withAdditionalMetadata("city", city)
                            .withAdditionalMetadata("type", docType)
                            .withAdditionalMetadata("filename", filename)
                            .build();
                    MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
                    allDocuments.addAll(markdownDocumentReader.get());
                }
            } catch (IOException e) {
                log.debug("文档加载失败: document/" + city + "/*.md");
            }
        }

        log.info("共加载文档 {} 篇", allDocuments.size());
        return allDocuments;
    }

    /**
     * 加载所有城市的文档（全部城市，用于生产环境或需要全量知识库时）
     * @return 文档列表
     */
//    public List<Document> loadAllMarkdowns() {
//        List<Document> allDocuments = new ArrayList<>();
//        try {
//            Resource[] resources = resourcePatternResolver.getResources("classpath:document/**/*.md");
//            for (Resource resource : resources) {
//                String filename = resource.getFilename();
//                if (filename == null) continue;
//
//                String path = resource.getURL().getPath();
//                String[] parts = path.split("/");
//                String city = "";
//                String docType = "";
//
//                if (parts.length >= 2) {
//                    int idx = parts.length - 1;
//                    if (idx > 0) docType = parts[idx].replace(".md", "");
//                    if (idx > 1) city = parts[idx - 1];
//                }
//
//                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
//                        .withHorizontalRuleCreateDocument(true)
//                        .withIncludeCodeBlock(false)
//                        .withIncludeBlockquote(false)
//                        .withAdditionalMetadata("city", city)
//                        .withAdditionalMetadata("type", docType)
//                        .withAdditionalMetadata("filename", filename)
//                        .build();
//                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
//                allDocuments.addAll(markdownDocumentReader.get());
//            }
//            log.info("共加载文档 {} 篇（全部城市）", allDocuments.size());
//        } catch (IOException e) {
//            log.error("Markdown 文档加载失败", e);
//        }
//        return allDocuments;
//    }
}