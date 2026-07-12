# Phase 3 Error Analysis Flow

## 本阶段目标

实现 `POST /api/error/analyze`，让用户输入 Java Web 报错文本后，系统可以：

1. 根据 MySQL `error_case.keyword` 和故障名称做规则匹配。
2. 命中后读取 Neo4j 中对应故障图谱。
3. 返回结构化排障结果，包括环节、组件、现象、原因、方案、相关文件、相关命令和图谱。
4. 将本次分析写入 MySQL `analysis_history`。
5. 前端在“智能排障”页面展示分析结果和关系图。

## 后端改动

新增包：

```text
backend/src/main/java/com/mistmoe/troubleshoot/analyze/
```

核心文件：

```text
AnalyzeRequest.java
AnalyzeResponse.java
AnalysisHistoryRepository.java
ErrorAnalysisService.java
ErrorAnalysisController.java
```

新增接口：

```http
POST /api/error/analyze
```

请求示例：

```json
{
  "inputText": "Web server failed to start. Port 8080 already in use.",
  "techStack": "Spring Boot",
  "userId": 3
}
```

当前阶段的建议文本仍是基于知识库字段拼出的确定性建议。真正调用大模型生成建议放到 Phase 4。

## 匹配规则

当前采用轻量规则：

1. 输入文本必须命中 `error_case.keyword` 或 `error_case.error_name`。
2. `techStack` 只作为加分项，不能单独造成命中。
3. 多个案例命中时选择分数最高的案例。
4. 未命中时返回 `matched=false`，并提示用户补充完整日志、版本、配置和复现步骤。

这样可以避免用户只选了 “Spring Boot” 就误命中任意 Spring Boot 故障。

## 数据库改动

新增 MySQL 表：

```text
analysis_history
```

字段包括：

```text
user_id
input_text
tech_stack
matched_error_id
matched_error_name
graph_result
ai_advice
create_time
```

已同步更新：

```text
database/mysql_schema.sql
backend/src/test/resources/schema.sql
```

并已在本机 `troubleshoot_kg` 数据库中执行幂等建表，确认表存在。

## 前端改动

`frontend/src/App.vue` 新增“智能排障”可用页面：

1. 错误日志输入框。
2. 技术栈选择器。
3. “开始分析”按钮。
4. 命中状态、故障名称、环节、组件、原因、方案、文件、命令展示。
5. 当前阶段建议文本展示。
6. 若后端返回图谱节点，则用 ECharts 展示关系图。

`frontend/src/styles.css` 新增分析页面布局样式。

## 验证结果

后端新增测试：

```text
backend/src/test/java/com/mistmoe/troubleshoot/analyze/ErrorAnalysisControllerTest.java
```

验证内容：

1. 端口占用日志可以命中知识库案例并写入历史表。
2. 未知日志不会只因为选择技术栈而误命中。

已执行：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$repo='C:\Users\97780\.m2\repository'
..\tools\apache-maven-3.9.16\bin\mvn.cmd "-Dmaven.repo.local=$repo" -o test
```

结果：

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

前端已执行：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

结果：构建成功。仍有 Vite chunk 体积警告，原因是 Element Plus 和 ECharts 打包体积较大，不影响当前运行。

## 后续衔接

Phase 4 可以直接复用 `AnalyzeResponse` 中的结构化字段和 `graph`：

1. 新增 LLM 配置占位，不硬编码密钥。
2. 新增 LLM Client Service。
3. 将 `inputText`、匹配到的故障字段、Neo4j 图谱结果组装为 Prompt。
4. 用 LLM 生成 `aiAdvice`，失败时回退到当前 Phase 3 的知识库建议。
5. 历史表无需大改，可以继续保存最终 `ai_advice`。

Phase 5 做历史页面时可以基于 `analysis_history` 增加：

```http
GET /api/history/list
GET /api/history/my
```
