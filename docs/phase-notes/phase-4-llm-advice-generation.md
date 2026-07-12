# Phase 4 LLM Advice Generation

## 本阶段目标

在 Phase 3 的错误分析流程上接入大模型建议生成能力，让 `POST /api/error/analyze` 的 `aiAdvice` 不再只能来自固定模板，而是可以在启用配置后由 LLM 基于知识图谱检索结果生成。

本阶段仍然遵守项目边界：

1. 不硬编码真实 API Key。
2. LLM 必须基于 MySQL 知识库和 Neo4j 图谱结果生成建议。
3. 如果未配置密钥或调用失败，系统必须回退到知识库建议。

## 后端改动

新增包：

```text
backend/src/main/java/com/mistmoe/troubleshoot/llm/
```

核心文件：

```text
LlmProperties.java
LlmAdviceService.java
```

`ErrorAnalysisService` 已改为：

```text
匹配故障案例 -> 查询 Neo4j 图谱 -> 调用 LlmAdviceService 生成 aiAdvice -> 保存 analysis_history
```

## 配置说明

默认配置放在：

```text
backend/src/main/resources/application.yml
```

当前默认关闭 LLM：

```yaml
llm:
  enabled: false
  provider: deepseek
  api-url: https://api.deepseek.com/chat/completions
  api-key: ${LLM_API_KEY:}
  model: deepseek-chat
```

启用方式：

1. 设置环境变量 `LLM_API_KEY`。
2. 将 `llm.enabled` 改为 `true`。
3. 如需使用其他 OpenAI-compatible 服务，修改 `llm.api-url` 和 `llm.model`。

示例：

```powershell
$env:LLM_API_KEY='你的真实 API Key'
```

不要把真实 Key 写入文档、截图或提交记录。

## Prompt 设计

系统提示词要求模型：

1. 作为 Java Web troubleshooting assistant。
2. 主要依据结构化知识库和 Neo4j 图谱结果。
3. 不编造不受支持的方案。
4. 优先给出最可能原因。
5. 信息不足时要求用户补充版本、配置和完整日志。

用户提示词包含：

```text
inputText
techStack
errorName
stage
component
symptom
cause
solution
relatedFile
relatedCommand
graph nodes
graph links
```

## 回退策略

以下情况不会中断排障流程：

1. `llm.enabled=false`
2. `llm.api-key` 为空
3. `llm.api-url` 为空
4. LLM API 调用异常
5. LLM 返回内容为空

系统会使用知识库兜底建议，并继续写入 `analysis_history.ai_advice`。

## 测试

新增测试：

```text
backend/src/test/java/com/mistmoe/troubleshoot/llm/LlmAdviceServiceTest.java
```

更新测试：

```text
backend/src/test/java/com/mistmoe/troubleshoot/analyze/ErrorAnalysisControllerTest.java
```

验证内容：

1. `POST /api/error/analyze` 会使用 `LlmAdviceService` 返回的建议。
2. LLM 未启用时会返回知识库兜底建议。

已执行：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$repo='C:\Users\97780\.m2\repository'
..\tools\apache-maven-3.9.16\bin\mvn.cmd "-Dmaven.repo.local=$repo" -o "-Dtest=ErrorAnalysisControllerTest,LlmAdviceServiceTest" test
```

结果：

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 后续衔接

Phase 5 可以在前端继续优化：

1. 在分析结果页标注建议来源：LLM / 知识库回退。
2. 做分析历史列表，从 `analysis_history` 读取历史。
3. 做演示用案例预设，一键填充常见日志。
4. 如果老师要求展示 Prompt，可以增加一个“查看检索证据”区域。
