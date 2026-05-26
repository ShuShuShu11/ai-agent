# 问题记录 (Issue Log)

---

## 问题 1：JDK 版本不支持

**日期：** 2026-05-26

### 问题描述

运行 `./mvnw spring-boot:run` 时编译失败：

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile
Fatal error compiling: 错误: 不支持发行版本 21
```

**原因：** 系统安装的 JDK 版本低于项目要求的 Java 21

### 解决方法

修改 `pom.xml`，将 Java 版本从 21 改为 17（与本机 JDK 版本匹配）：

```xml
<!-- 修改前 -->
<java.version>21</java.version>

<!-- 修改后 -->
<java.version>17</java.version>
```

### 经验总结

- 遇到 "不支持发行版本 XX" 错误，先用 `java -version` 确认本机 JDK 版本
- 项目 Java 版本需 ≤ 本机 JDK 版本
- 可以通过修改 pom.xml 解决，或者升级本机 JDK 到更高版本

---

## 问题 2：Maven Wrapper (mvnw) 是什么？为什么不需要手动下载依赖？

**日期：** 2026-05-26

### 问题描述

疑惑：运行 `./mvnw` 就能启动项目，不需要预先安装 Maven，也不需要手动下载依赖，这是怎么做到的？

### 原因分析

**1. Maven Wrapper 是什么？**
- 项目自带的 Maven 客户端脚本（`mvnw` / `mvnw.cmd`）
- 包含在项目中，无需单独安装 Maven
- 首次运行时会自动下载 Maven 二进制包到 `.mvn` 目录

**2. 工作原理：**
```
运行 ./mvnw spring-boot:run
       ↓
读取 .mvn/wrapper/maven-wrapper.properties
       ↓
下载指定版本的 Maven（从 distributionUrl）
       ↓
保存到 ~/.m2/wrapper/dists/...
       ↓
Maven 读取 pom.xml 中的 dependencies
       ↓
从仓库下载依赖到 ~/.m2/repository
       ↓
编译 → 运行
```

**3. 依赖下载到哪里了？**
- Windows: `C:\Users\evil\.m2\repository`
- 包含所有下载的 JAR 包（spring、alibaba 等）

### 关键文件

| 文件 | 作用 |
|------|------|
| `mvnw` / `mvnw.cmd` | Linux/Mac 或 Windows 的启动脚本 |
| `.mvn/wrapper/maven-wrapper.properties` | 指定 Maven 版本和下载 URL |
| `pom.xml` | 定义项目依赖和构建配置 |

### 经验总结

- Maven Wrapper 让项目开箱即用，无需开发人员预先安装 Maven
- 依赖是自动下载的，每次运行 `./mvnw` 时 Maven 会检查并下载缺失的依赖
- 已下载的依赖缓存在本地 `.m2` 目录，不会重复下载

---

## 问题 3：AI超级智能体 (huhuManus) 无工具调用时陷入死循环

**日期：** 2026-05-26

### 问题描述

用户对 AI 超级智能体说"你好"，AI 回复欢迎语后就不停循环，每次都是"思考完成 - 无需行动"，直到达到最大步骤 20 次后结束。

### 原因分析

1. **think() 返回 false 后，step() 没有返回 AI 的实际响应**
   - 当 AI 判断不需要调用工具时，`think()` 返回 false
   - 但 `ReActAgent.step()` 只返回"思考完成 - 无需行动"，没有返回 AI 的实际回复内容
   - AI 回复内容被存储在 `ToolCallAgent.lastNoToolResponse` 中，但没有使用

2. **循环逻辑问题**
   - `BaseAgent.run()` 循环执行 `step()`，每次都记录结果
   - 当 `think()` 返回 false 时，应该立即结束而不是继续循环

### 解决方法

1. `ToolCallAgent.java` 新增字段 `lastNoToolResponse`，在 `think()` 返回 false 时记录 AI 响应
2. `ReActAgent.java` 的 `step()` 方法检查 `think()` 返回 false 时，获取并返回 `lastNoToolResponse`

### 关键代码

```java
// ToolCallAgent.java
private String lastNoToolResponse; // 新增字段

// ReActAgent.java
@Override
public String step() {
    boolean shouldAct = think();
    if (!shouldAct) {
        if (this instanceof ToolCallAgent) {
            ToolCallAgent toolAgent = (ToolCallAgent) this;
            String response = toolAgent.getLastNoToolResponse();
            if (StrUtil.isNotBlank(response)) {
                return response; // 返回 AI 实际响应
            }
        }
        return "思考完成 - 无需行动";
    }
    return act();
}
```

### 经验总结

- ReAct 模式下，当 AI 不需要工具调用时，应该直接返回 AI 的响应而不是陷入循环
- 需要区分"思考后决定不行动"和"思考后给出答案"两种情况

---

## 问题 4：Spring AI 1.0+ API 变化 - `.tools()` 改为 `.toolCallbacks()`

**日期：** 2026-05-26

### 问题描述

运行时报错：
```
No @Tool annotated methods found in MethodToolCallback... Did you mean to pass a ToolCallback or ToolCallbackProvider? If so, you have to use .toolCallbacks() instead of .tool()
```

### 原因分析

Spring AI 1.0+ 版本中，传递 `ToolCallback` 数组的 API 发生了变化：
- 旧版: `.tools(availableTools)`
- 新版: `.toolCallbacks(availableTools)`

### 解决方法

修改 `ToolCallAgent.java` 第 75 行：
```java
// 修改前
.tools(availableTools)
// 修改后
.toolCallbacks(availableTools)
```

### 经验总结

- 使用 Spring AI 时注意版本差异，API 可能有 breaking change
- 查看 [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/) 确认最新 API

---

## 问题 5：WebScrapingTool 返回 HTML 而不是纯文本

**日期：** 2026-05-26

### 问题描述

`scrapeWebPage` 工具返回的是百度首页的完整 HTML 源码（包括 CSS 样式、JS 脚本等），而不是用户可见的文本内容。

### 原因分析

1. `document.html()` 返回的是完整的 HTML 字符串，包括 `<style>` 标签内的 CSS 内容
2. 百度首页内容巨大（热榜搜索等），直接返回会导致内容过长

### 解决方法

修改 `WebScrapingTool.java`：
```java
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
```

### 经验总结

- 网页抓取应返回纯文本，避免 HTML/CSS 代码干扰
- 大页面需要截断，避免超出模型上下文限制
- 移除 script/style 等无意义标签

---

## 问题 6：PDFGenerationTool 中文字体 STSongStd-Light 不可用

**日期：** 2026-05-26

### 问题描述

生成 PDF 时报错：`Type of font STSongStd-Light is not recognized.`

### 原因分析

iText 中文字体 `STSongStd-Light` 需要额外的字体文件支持，或者字体名称不正确。

### 解决方法

修改 `PDFGenerationTool.java`，添加字体 fallback 机制：
```java
PdfFont font;
try {
    font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
} catch (Exception e) {
    // 如果中文字体不可用，使用 Helvetica
    font = PdfFontFactory.createFont(Helvetica.FONT);
}
document.setFont(font);
```

### 经验总结

- iText 生成中文 PDF 需要中文字体文件（如 STSongStd-Light）
- 如果字体不可用，应添加 fallback 机制避免程序崩溃
- 可以预先下载中文字体文件到资源目录

---

## 问题 7：WebScrapingTool SSL 证书错误

**日期：** 2026-05-26

### 问题描述

访问某些国内网站（如 `nmc.cn`）时出现 SSL 证书错误：
```
Error scraping web page: (certificate_unknown) PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

### 原因分析

Java 默认启用了严格的 SSL 证书验证，某些网站的证书不被信任（自签名证书、过期的证书链等）。

### 解决方法

在 `WebScrapingTool.java` 中禁用 SSL 证书验证（仅用于开发/测试）：
```java
Document document = Jsoup.connect(url)
    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    .timeout(10000)
    .ignoreHttpErrors(true)
    .sslSocketFactory(信任所有证书的 SocketFactory) // 需要额外配置
    .get();
```

或使用 `TrustManager` 跳过验证（仅用于开发环境）。

### 经验总结

- 生产环境不应禁用 SSL 验证
- 这个问题说明国内部分网站证书配置有问题
- 可以考虑切换到 HTTP 而不是 HTTPS

---

## 问题 8：AI Agent 任务结束时没有最终回答用户的问题

**日期：** 2026-05-26

### 问题描述

用户问"今天杭州的天气"，AI 成功获取了天气信息，但在调用 `doTerminate` 结束任务前，没有给用户一个最终的总结性回答。

执行流程：
1. searchWeb → 获取搜索结果
2. scrapeWebPage → 获取天气数据
3. doTerminate → 直接结束

### 原因分析

当前 Agent 的 `step()` 循环中：
- 当 `think()` 返回不需要工具调用时，直接返回 AI 响应
- 当所有工具调用完成后，最后一步 AI 判断"任务完成"后调用 `doTerminate`
- 但没有额外的步骤让 AI 生成最终回答给用户

### 解决方法

需要让 AI 在调用 `doTerminate` 前，生成一个最终回答。可以：
1. 在 `TerminateTool.doTerminate()` 中加入最终回答的处理
2. 或者在 `BaseAgent` 的循环结束逻辑中，检查是否有最终响应需要返回

### 经验总结

- 自主规划智能体的"结束"和"回答用户"需要区分清楚
- 应该在给出最终答案后再调用 terminate，而不是 terminate 后就不再响应

---

## 模板

```markdown
## 问题 N：标题

**日期：** YYYY-MM-DD

### 问题描述

报错信息或现象

**原因：** 分析

### 解决方法

1. 步骤 1
2. 步骤 2

### 经验总结

- 关键点 1
- 关键点 2
```