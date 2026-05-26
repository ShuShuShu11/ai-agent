package com.huhuhu.aiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;

/**
 * 网页抓取工具
 */
public class WebScrapingTool {

    // 用于跳过 SSL 证书验证的 TrustManager（仅开发环境使用）
    private static final TrustManager[] UNCONDITIONAL_TRUST_MANAGER = new TrustManager[]{
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }
    };

    @Tool(description = "Scrape the text content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            // 创建信任所有证书的 SSL SocketFactory（仅开发环境使用）
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, UNCONDITIONAL_TRUST_MANAGER, new SecureRandom());

            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .sslSocketFactory(sslContext.getSocketFactory())
                    .get();

            // 彻底移除所有 style 和 script 标签
            document.getElementsByTag("style").remove();
            document.getElementsByTag("script").remove();
            document.getElementsByTag("noscript").remove();
            document.getElementsByTag("link").remove();
            document.getElementsByTag("meta").remove();

            // 获取 body 的纯可见文本
            String text = document.body().text();

            // 清理多余空白
            text = text.replaceAll("\\s+", " ").trim();

            // 限制内容长度
            if (text.length() > 8000) {
                text = text.substring(0, 8000) + "\n... (内容已截断)";
            }
            return text;
        } catch (Exception e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
