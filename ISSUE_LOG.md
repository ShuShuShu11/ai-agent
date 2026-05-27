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

## 问题 9：恋爱大师 → 浙江旅游助手 重构

**日期：** 2026-05-26

### 项目描述

将项目从"AI恋爱大师"重构为"浙江旅游助手"，所有内容、提示词、文档、注释、前端页面均需修改。文档内容以浙江省为主。

### 修改内容

**后端：**
- `LoveApp.java` → `TourismApp.java`（重命名+旅游系统提示词）
- `AiController.java`：端点从 `/love_app` 改为 `/tourism`
- RAG相关类全部重命名：`LoveApp*` → `Tourism*`
  - `LoveAppDocumentLoader.java` → `TourismDocumentLoader.java`
  - `LoveAppVectorStoreConfig.java` → `TourismVectorStoreConfig.java`
  - `LoveAppRagCustomAdvisorFactory.java` → `TourismRagCustomAdvisorFactory.java`
  - `LoveAppContextualQueryAugmenterFactory.java` → `TourismContextualQueryAugmenterFactory.java`
  - `LoveAppRagCloudAdvisorConfig.java` → `TourismRagCloudAdvisorConfig.java`
- 删除旧恋爱文档，新增3篇浙江旅游文档：
  - `浙江热门景点推荐.md`
  - `浙江特色美食攻略.md`
  - `浙江行程规划指南.md`
- 注释和类描述更新为旅游相关

**前端：**
- `LoveMaster.vue` → `Tourism.vue`
- `Home.vue`：移除"AI恋爱大师"卡片，保留"浙江旅游助手"和"AI超级智能体"
- `router/index.js`：移除 `/love-master` 路由
- `api/index.js`：移除 `chatWithLoveApp`，保留 `chatWithTourism` 和 `chatWithManus`

### 验证结果

- 后端编译：✅ BUILD SUCCESS
- 前端构建：✅ built in 1.78s
- 启动测试：✅ SSE 流式返回正常，AI 正确回复旅游助手欢迎语

### 经验总结

- 重构时注意同时修改前后端路由/端点保持一致
- 类名重命名后检查所有引用
- 删除旧文档后确认编译和构建均成功

---

## 问题 10：PDF 生成中文字体显示为乱码

**日期：** 2026-05-26

### 问题描述

`PDFGenerationTool` 生成的 PDF 中，中文字符全部显示为乱码（如 `&` `++` `6–7` 等符号字符），无法正常阅读。

### 原因分析

原代码使用 `PdfFontFactory.createFont()` 创建内置 Helvetica 字体（PDF 标准字体），不支持中文字符。中文字符被当作非标准字符处理，渲染失败变成乱码。

### 解决方法

1. **复制字体文件到项目**
   ```bash
   cp C:/Windows/Fonts/simsun.ttc src/main/resources/fonts/
   ```

2. **修改 `PDFGenerationTool.java`**
   ```java
   private PdfFont createChineseFont() throws IOException {
       // 使用 classpath 中的中文字体（宋体）
       return PdfFontFactory.createFont("/fonts/simsun.ttc,0");
   }
   ```

3. **保留换行格式**
   ```java
   String[] lines = content.split("\n");
   for (String line : lines) {
       document.add(new Paragraph(line));
   }
   ```

### 验证结果

- ✅ 中文内容（标题、段落）显示正确
- ✅ 相对路径 `/fonts/simsun.ttc,0` 正常工作
- ⚠️ emoji（✨🎉🌅等）不支持，因为 simsun.ttc 是中文字体，不含 emoji 字形

### 经验总结

- iText 生成中文 PDF 需要嵌入中文字体（Helvetica 不支持中文）
- 字体文件放在 `src/main/resources/` 下，使用 `/fonts/xxx` 相对路径加载
- 宋体（simsun.ttc）是 Windows 中文系统标配，第一个成功加载的中文字体
- TTC 字体集合格式为 `路径,索引`（如 `simsun.ttc,0` = 宋体常规）

---

## 问题 11：AI 不主动调用 searchWeb 查询天气/实时信息

**日期：** 2026-05-26

### 问题描述

用户问"杭州今天天气怎么样"，AI 反复回复"可以联网搜索获取实时天气"但不实际调用工具，而是生成通用建议。

### 原因分析

SYSTEM_PROMPT 中没有明确要求遇到天气/实时资讯时**必须调用工具**，AI 默认选择不调用（因为 toolChoice 是 auto 模式）。

### 解决方法

在 `TourismApp.java` 的 SYSTEM_PROMPT 末尾添加指令：
```
"重要规则：遇到天气、实时资讯等需要联网查询的问题，直接调用 searchWeb 工具获取信息，不要询问用户。"
```

### 验证结果

- ✅ 添加规则后，AI 收到天气问题自动调用 searchWeb
- ✅ 返回实时天气数据（2026年5月26日杭州大雨，27~30℃）

### 经验总结

- Spring AI 默认 toolChoice.auto()，AI 自己决定是否用工具
- 提示词中明确规则可以引导 AI 主动调用工具
- 规则表述要具体："直接调用 XXX 工具" 而非 "可以使用工具"

---

## 问题 12：前端未使用的接口和方法需要清理

**日期：** 2026-05-27

### 问题描述

`TourismApp.java` 中包含多个前端未调用的方法，导致代码冗余。

### 解决方法

1. **确认前端实际使用的接口：**
   - `chatWithTourism` → `/ai/tourism/chat/sse`
   - `chatWithTourismToolsAndRag` → `/ai/tourism/chat/with_tools_and_rag/sse`
   - `chatWithManus` → `/ai/manus/chat`

2. **删除前端未用的方法和接口：**
   - `doChat`（基础同步对话）
   - `doChatWithRag`（单独 RAG）
   - `doChatWithTools`（单独工具）
   - `doChatWithToolsStream`（工具 SSE）
   - `doChatWithMcp`（MCP 调用）
   - `doChatWithReport`（结构化报告）

3. **保留的方法：**
   - `doChatByStream` - 基础对话 SSE
   - `doChatWithToolsAndRagStream` - 工具+RAG SSE

### 验证结果

- ✅ 编译通过
- ✅ `TourismApp.java`: 187行 → 69行
- ✅ `AiController.java`: 97行 → 48行

---

## 问题 13：PgVectorStoreConfig 在无数据库时启动失败

**日期：** 2026-05-27

### 问题描述

未启用 PgVector 数据库时，应用启动失败：
```
UnsatisfiedDependencyException: Error creating bean with name 'tourismPgVectorStore'
Parameter 0 of method tourismPgVectorStore in TourismPgVectorStoreConfig required a bean of type 'JdbcTemplate'
```

### 解决方法

在 `TourismPgVectorStoreConfig` 添加条件注解：
```java
@Configuration
@ConditionalOnProperty(name = "spring.datasource.url")
public class TourismPgVectorStoreConfig {
```

只有配置了 `spring.datasource.url` 时才会加载 PgVectorStore 配置。

---

## 问题 14：RAG 强制路由 + ContextualQueryAugmenter 注入导致回答异常

**日期：** 2026-05-27

### 问题描述

1. **表现 A**：启用 RAG 后，即使文档中有相关内容（如"西湖门票多少钱"），AI 仍然回复"超出我的服务范围"
2. **表现 B**：`/sse` 可以正常闲聊，`/with_tools_and_rag/sse` 无法闲聊（如"你好我是噜噜"也会被拒绝）

### 原因分析（同一根因）

1. `ContextualQueryAugmenter` 是 Spring AI RAG 框架的内部组件，当它认为检索结果"不够好"或 context 为空时，会自动注入 `The user query is outside your knowledge base.` 到提示词中
2. 即使向量检索确实找到了文档（`ConcatenationDocumentJoiner` 有日志），`ContextualQueryAugmenter` 仍可能认为 context 不够好
3. `/with_tools_and_rag/sse` 路径**强制所有问题都走 RAG**，非旅游相关问题检索不到内容，更容易触发注入
4. AI 看到注入的错误提示后，认为应该拒绝回答，就产生了"超出服务范围"或"你是谁"被误解的问题

### 尝试的解决方法

| 方案 | 结果 |
|------|------|
| `similarityThreshold` 0.5→0.3，`topK` 3→5 | 未解决问题 |
| 禁用 `queryClassifier(null)` | API 不存在，编译失败 |
| 字符串替换清理错误提示 | LLM 是在看到提示后回答的，替换回答无效 |
| 自定义 `CleanUserMessageAdvisor` | Spring AI 1.0.0 API 不兼容 |

### 解决方案（已实施）

在 `doChatWithToolsAndRagStream` 中增加 `shouldUseRag()` 路由判断：

```java
private boolean shouldUseRag(String message) {
    // 个人/闲聊问题，不走 RAG
    if (message.matches(".*(叫什么|我是谁|记住|刚才|之前|我多少岁|我多大|你是谁|你好|嗨|哈喽).*")) {
        return false;
    }
    // 旅游相关关键字，走 RAG
    String[] tourismKeywords = {"景点", "美食", "酒店", "民宿", "门票", "西湖", "乌镇", "千岛湖", "杭州", "浙江", "旅游", "行程", "路线", "好玩", "推荐", "住宿", "交通", "天气"};
    for (String kw : tourismKeywords) {
        if (message.contains(kw)) {
            return true;
        }
    }
    return false;
}
```

- **走 RAG**：旅游相关问题（景点、美食、天气等）
- **不走 RAG**：闲聊、个人信息、记忆相关问题

---

## 问题 15：文档按城市维度重构 + SYSTEM_PROMPT 风格统一

**日期：** 2026-05-27

### 问题描述

1. 原文档结构为单层（浙江热门景点推荐、浙江特色美食攻略等），需要按城市维度重新组织
2. SYSTEM_PROMPT 风格太正式，与前端欢迎语"嗨！我是浙江旅游助手 呼呼"不一致
3. 前端未使用的接口和方法冗余

### 解决方法

1. **文档重构**：按浙江省11个地级市组织，每个市创建4个文件（景点、美食、交通、路线）
   - 11市：杭州、宁波、温州、嘉兴、湖州、绍兴、金华、衢州、舟山、台州、丽水
   - 共44个文档文件

2. **TourismDocumentLoader 优化**：
   - 支持按城市维度加载
   - 当前只加载杭州（加速启动）
   - 其他城市可通过修改 CITIES 数组逐步添加
   - 删除了旧根目录的3个文档

3. **SYSTEM_PROMPT 简化**：
   - 改为亲切随和的风格
   - 简洁明了，去掉冗长描述
   - 与欢迎语"呼呼"保持一致

4. **前端欢迎语**：
   - 改为"嗨！我是浙江旅游助手 呼呼～有什么关于浙江旅游的问题尽管问我吧！"

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