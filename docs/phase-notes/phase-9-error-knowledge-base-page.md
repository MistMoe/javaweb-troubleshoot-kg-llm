# Phase 9 故障知识库页

## 本阶段目标

本阶段实现登录后的“故障知识库页”，用于维护 MySQL `error_case` 表中的故障案例。

本阶段只改前端页面，没有改后端业务逻辑，也没有修改登录页、注册页、首页工作台或全局 AI 助手视觉。

## 页面定位

故障知识库页是智能排障和知识图谱的上游数据维护页：

```text
故障知识库 error_case
  -> 智能排障 /api/error/analyze
  -> Neo4j 图谱导入 /api/kg/import
  -> 知识图谱页展示
```

因此它做完后，后续知识图谱页可以直接基于这页维护的数据继续展示。

## 修改文件

新增：

- `frontend/src/views/knowledge/KnowledgeBaseView.vue`
- `frontend/src/views/knowledge/knowledge-base-view.css`

修改：

- `frontend/src/App.vue`
- `README.md`

## 已接入真实接口

| 功能 | 接口 |
| --- | --- |
| 查询故障案例 | `GET /api/error/list?keyword=&component=&stage=` |
| 新增故障案例 | `POST /api/error/create` |
| 编辑故障案例 | `PUT /api/error/{id}` |
| 删除故障案例 | `DELETE /api/error/{id}` |
| 同步 Neo4j 图谱 | `POST /api/kg/import` |

## 页面功能

当前页面已经支持：

- 故障案例列表加载。
- 按关键词、技术组件、发生环节筛选。
- 选择案例查看详情。
- 新增案例。
- 编辑案例。
- 删除案例。
- 同步到 Neo4j。
- 前端根据当前列表聚合：
  - 案例总数。
  - 技术组件数量。
  - 发生环节数量。
  - 组件分布折线图。

## 本阶段追加微调

根据页面预览反馈，做了如下布局修正：

- 原本右侧“案例详情”没有从筛选区右侧开始，导致右上区域空着。
- 原本“组件分布”作为独立卡片被 CSS Grid 自动排到页面左下方，视觉上像掉出主布局。
- 调整后，“案例详情”固定在右栏，从筛选区这一行开始并跨两行展示。
- 删除独立的“组件分布”卡片。
- 将组件分布改为轻量折线图，并嵌入“案例详情”卡片顶部。

这样右侧栏信息更连续，也避免页面底部出现孤立的小卡片。

## 视觉风格

页面延续登录后系统的整体风格：

- 清冷白色工作台。
- 轻浮雕卡片。
- 极细线边框。
- 淡蓝 / 淡紫点缀。
- 非传统后台大表格。
- 列表 + 详情预览 + 轻量统计的拼贴布局。

## 当前真实与静态边界

真实功能：

- 列表查询。
- 条件筛选。
- 新增、编辑、删除。
- 同步 Neo4j。
- 统计数据由真实列表在前端聚合。

暂未实现：

- 后端分页。
- 批量导入 / 导出。
- 字典接口。
- 版本记录或审计日志。
- 删除后自动清理 Neo4j 旧节点。

## 验证结果

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

布局微调后再次执行前端构建，结果同样通过。

故障知识库后端接口测试：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$repo='C:\Users\97780\.m2\repository'
..\tools\apache-maven-3.9.16\bin\mvn.cmd "-Dmaven.repo.local=$repo" -o -Dtest=ErrorCaseControllerTest test
```

结果：

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

另外，开始本阶段前已补跑后端全量测试：

```text
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 后续衔接

下一步推荐做“知识图谱页”。

原因：

- 故障知识库页已经能维护 `error_case`。
- 当前页面已经提供“同步到 Neo4j”的入口。
- 后端已有图谱导入、图谱查询、节点搜索接口。

下一页可用接口：

- `POST /api/kg/import`
- `GET /api/kg/graph/{errorId}`
- `GET /api/kg/search?keyword=...`
- `GET /api/error/list`

下一页建议实现：

- 案例选择。
- 图谱搜索。
- 关系图展示。
- 节点详情侧栏。
- 节点 / 关系统计。
- 图谱导入状态提示。
