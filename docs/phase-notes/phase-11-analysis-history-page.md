# Phase 11 分析历史页

## 本阶段目标

本阶段完成登录后的“分析历史 / 我的分析历史”页面，让智能排障产生的 `analysis_history` 记录可以被查询、复盘和删除。

这一页用于补全排障闭环：

```text
智能排障输入
  -> 匹配 MySQL 故障知识库
  -> 查询 Neo4j 图谱证据
  -> 生成 AI 建议
  -> 写入 analysis_history
  -> 分析历史页复盘
```

## 修改文件

新增：

- `backend/src/main/java/com/mistmoe/troubleshoot/analyze/AnalysisHistoryController.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/analyze/AnalysisHistoryService.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/analyze/AnalysisHistorySummary.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/analyze/AnalysisHistoryDetail.java`
- `backend/src/test/java/com/mistmoe/troubleshoot/analyze/AnalysisHistoryControllerTest.java`
- `frontend/src/views/history/HistoryView.vue`
- `frontend/src/views/history/history-view.css`

修改：

- `backend/src/main/java/com/mistmoe/troubleshoot/analyze/AnalysisHistoryRepository.java`
- `frontend/src/App.vue`
- `README.md`

## 新增后端接口

| 功能 | 接口 | 说明 |
| --- | --- | --- |
| 查询分析历史列表 | `GET /api/analysis-history/list` | 支持按 `userId`、`keyword`、`matched` 过滤 |
| 查询分析历史详情 | `GET /api/analysis-history/{id}` | 返回完整输入、AI 建议和图谱 JSON |
| 删除分析历史 | `DELETE /api/analysis-history/{id}` | 删除单条历史记录 |

列表接口返回的是摘要数据，包含：

- 用户信息：`userId`、`username`、`nickname`
- 输入摘要：`inputText`
- 技术栈：`techStack`
- 命中信息：`matchedErrorId`、`matchedErrorName`、`matched`
- AI 建议：`aiAdvice`
- 图谱统计：`graphNodeCount`、`graphLinkCount`
- 时间：`createTime`

详情接口返回完整图谱：

- `graph.nodes`
- `graph.links`

前端可以用它做图谱快照或后续详情可视化。

## 页面功能

分析历史页当前支持：

- 管理员 / 维护员查看全部分析历史。
- 普通用户在“我的分析历史”中默认只查看自己的记录。
- 关键词搜索：可搜原始报错、技术栈、命中故障名、AI 建议。
- 命中状态筛选：全部 / 已命中知识库 / 未命中。
- 左侧时间线式历史列表。
- 右侧复盘详情：
  - 命中案例
  - 技术栈
  - 提交用户
  - 原始报错输入
  - AI 建议
  - 图谱节点快照
- 删除单条分析历史。

## 视觉设计

用户希望这一页“稍微整点颜色”，所以本阶段在保持清冷白色浮雕风格的基础上加入低饱和彩色点缀：

- 顶部概览使用蓝 / 紫 / 青三个指标胶囊。
- 技术栈分布使用彩色横向进度条。
- 时间线节点按技术栈区分颜色：
  - Spring Boot：绿色
  - MySQL：蓝色
  - Neo4j：紫色
  - Maven：橙色
  - Vue：青色
  - LLM API：粉色
- 详情区的原始输入和 AI 建议用不同颜色的左侧细线区分。
- 图谱快照节点按类型使用浅色标签。

整体仍然避免高饱和、霓虹和传统后台重表格。

## 权限衔接

当前前端已有菜单角色区分：

- `history`：ADMIN / MAINTAINER 可见，用于查看全部分析历史。
- `my-history`：USER 可见，用于查看自己的分析历史。

本阶段前端在 `scope === 'my-history'` 时自动带上当前登录用户 `userId` 查询。

注意：当前后端接口还没有做强权限拦截，属于课程演示阶段的轻量实现。后续如果做用户管理 / 角色管理页，可以补充后端权限校验。

## TDD 过程

先新增后端测试：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$repo='C:\Users\97780\.m2\repository'
..\tools\apache-maven-3.9.16\bin\mvn.cmd "-Dmaven.repo.local=$repo" -o -Dtest=AnalysisHistoryControllerTest test
```

RED 阶段结果：

```text
Tests run: 4, Failures: 4
Status expected:<200> but was:<404>
```

原因：`/api/analysis-history/*` 接口尚未实现。

GREEN 阶段实现接口后结果：

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 验证结果

后端新增测试：

```text
AnalysisHistoryControllerTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

前端构建：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

结果：

```text
vite build success
```

说明：仍有 Vite chunk 体积提示和第三方注释提示，不影响当前运行。

## 后续衔接

下一步建议做“用户管理页”。

原因：

- 分析历史页已经能体现不同角色的入口差异。
- 用户管理页可以承接“谁能登录、谁是什么角色”的问题。
- 用户管理做好后，角色管理页只需要做轻量权限说明和配置展示即可。

用户管理页可能需要新增接口：

- `GET /api/users/list`
- `POST /api/users/create`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}` 或禁用用户接口
- `PUT /api/users/{id}/roles`

角色管理页建议先轻量化：

- 展示 ADMIN / MAINTAINER / USER 三个角色。
- 展示每个角色能访问的模块。
- 不做复杂权限矩阵拖拽，避免偏离智能排障主线。
