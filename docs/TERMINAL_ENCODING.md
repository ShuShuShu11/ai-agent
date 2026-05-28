# VSCode 终端中文乱码解决

## 问题

VSCode 终端中 Java 打印的中文显示为乱码（如 `浣犲ソ` 而不是 `你好`）。

## 原因

Windows 默认编码是 GBK，不是 UTF-8。

## 解决方案

### 方案 1：临时设置编码（推荐）

每次开终端后先执行：
```bash
chcp 65001
```

然后再运行 Java 程序。

### 方案 2：永久设置 JAVA_TOOL_OPTIONS

```bash
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
```

### 方案 3：VSCode 配置（最方便）

在 VSCode 中按 `Ctrl+Shift+P`，输入 `settings.json`，选择"打开用户设置 JSON"，添加：

```json
"terminal.integrated.env.windows": {
    "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8"
}
```

### 方案 4：修改 mvnw.cmd

在 `mvnw.cmd` 文件开头（`@REM === Windows>` 之后）添加：

```batch
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
```

---

# 后端启动命令

## 启动后端

```bash
# 编译打包（跳过测试）
./mvnw clean package -DskipTests

# 启动指定端口
./mvnw spring-boot:run 
```