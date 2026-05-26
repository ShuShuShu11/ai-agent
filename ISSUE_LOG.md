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