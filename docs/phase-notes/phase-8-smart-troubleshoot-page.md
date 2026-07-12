# Phase 8 千帆 LLM 接入骨架与智能排障页

## 本阶段目标

本阶段完成两件事：

1. 后端补上千帆 / 文心一言 LLM Provider 配置骨架。
2. 前端新增登录后的“智能排障页”，并接入真实 `POST /api/error/analyze` 接口。

本阶段没有修改登录页、注册页视觉，也没有重做首页工作台。

## 后端改动

### 修改文件

- `backend/src/main/java/com/mistmoe/troubleshoot/llm/LlmProperties.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/llm/LlmAdviceService.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/mistmoe/troubleshoot/llm/LlmAdviceServiceTest.java`

### 新增能力

`LlmProperties` 增加千帆相关配置：

- `llm.qianfan-access-key`
- `llm.qianfan-secret-key`
- `llm.qianfan-token-url`
- `llm.qianfan-chat-url`
- `llm.qianfan-model`

`LlmAdviceService` 现在支持两类 Provider：

- 默认通用 OpenAI-compatible HTTP Provider。
- `qianfan` Provider。

调用策略：

1. `llm.enabled=false` 时不调用真实 LLM，直接返回知识库兜底建议。
2. `llm.provider=qianfan` 且千帆 AK/SK 配置齐全时，先获取 OAuth access token，再调用千帆聊天接口。
3. LLM 调用失败、未配置或返回空内容时，继续返回知识库兜底建议。

安全约束：

- 老师提供的账密不写入代码。
- 老师提供的账密不写入 README。
- 后续应通过环境变量或本地未提交配置文件提供：
  - `QIANFAN_ACCESS_KEY`
  - `QIANFAN_SECRET_KEY`
  - `QIANFAN_MODEL`

## 前端改动

### 新增文件

- `frontend/src/views/analyze/AnalyzeView.vue`
- `frontend/src/views/analyze/analyze-view.css`

### 修改文件

- `frontend/src/App.vue`

### 页面能力

智能排障页现在包括：

- 报错日志输入区。
- 技术栈选择。
- 常用样例快捷填充。
- 调用真实 `POST /api/error/analyze`。
- 展示匹配状态。
- 展示匹配故障名称、阶段、消息。
- 展示置信度、图谱节点数、关系数。
- 展示可能原因、解决方案。
- 展示相关文件、建议命令。
- 展示 Neo4j 图谱证据。
- 展示 AI 排障建议。

## 当前真实链路

页面提交后执行：

```text
AnalyzeView
  -> POST /api/error/analyze
  -> ErrorAnalysisService
  -> ErrorCaseRepository 匹配 MySQL error_case
  -> KgService 查询 Neo4j graph
  -> LlmAdviceService 生成 AI 建议或知识库兜底建议
  -> AnalysisHistoryRepository 写入 analysis_history
  -> AnalyzeView 展示结果
```

这意味着智能排障页已经不是纯静态页面，它会使用真实后端接口，并写入分析历史。

## 验证结果

已执行：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$repo='C:\Users\97780\.m2\repository'
..\tools\apache-maven-3.9.16\bin\mvn.cmd "-Dmaven.repo.local=$repo" -o -Dtest=LlmAdviceServiceTest test
```

结果：

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

已执行：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

结果：

```text
vite build success
```

说明：

- 前端构建存在 Vite chunk 体积提示，这是当前 Element Plus / ECharts 打包体积导致的提示，不影响运行。
- 后端全量测试本次因授权/额度限制未能启动；已完成 LLM 单测验证。

## 后续衔接

下一步推荐做“故障知识库页”。

原因：

- 知识库页后端 CRUD 已经具备。
- 智能排障页依赖 `error_case` 数据，知识库页做好后可以直接维护排障数据。
- 知识库页做好后，再做知识图谱页会更顺，因为图谱从 MySQL 知识库导入。

下一页开始前需要说明：

- 页面目标：维护故障案例。
- 真实接口：`GET /api/error/list`、`POST /api/error/create`、`PUT /api/error/{id}`、`DELETE /api/error/{id}`。
- 数据源：MySQL `error_case`。
- 先静态部分：统计图、热度、批量导入导出。
- 可能补后端：分页、批量导入、字典接口。
