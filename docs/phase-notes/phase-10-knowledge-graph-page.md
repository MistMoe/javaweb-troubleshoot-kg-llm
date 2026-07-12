# Phase 10 知识图谱页

## 本阶段目标

本阶段完成登录后的“知识图谱页”，用于查看 MySQL 故障案例导入 Neo4j 后形成的故障关系图。

同时根据页面反馈，顺手调整了故障知识库页中的“组件分布走势”小图，让它更像轻量趋势图，而不是拥挤的折线坐标图。

## 修改文件

新增：

- `frontend/src/views/graph/KnowledgeGraphView.vue`
- `frontend/src/views/graph/knowledge-graph-view.css`

修改：

- `frontend/src/App.vue`
- `frontend/src/views/knowledge/KnowledgeBaseView.vue`
- `frontend/src/views/knowledge/knowledge-base-view.css`
- `README.md`

## 知识图谱页接入接口

| 功能 | 接口 |
| --- | --- |
| 查询故障案例 | `GET /api/error/list` |
| 从 MySQL 导入 Neo4j | `POST /api/kg/import` |
| 查询某个故障案例图谱 | `GET /api/kg/graph/{errorId}` |
| 搜索图谱节点 | `GET /api/kg/search?keyword=...` |

## 页面功能

当前知识图谱页支持：

- 加载故障案例列表。
- 默认选择第一个故障案例并尝试加载图谱。
- 手动从 MySQL 同步图谱到 Neo4j。
- 按故障案例查看 1 到 3 跳关系图。
- 按关键字搜索图谱节点。
- 展示节点数、关系数、节点类型数。
- 图谱节点点击后在右侧展示节点详情。
- 显示节点相关连线数量。
- 图谱可缩放、拖拽、拖动节点。

## 图谱展示优化

为了避免图谱挤在一起，本阶段做了这些处理：

- 图谱主画布高度设置为 `680px`。
- 图谱主卡片最小高度设置为 `760px`。
- ECharts force layout 使用更大的斥力：
  - `repulsion: 560`
  - `edgeLength: [150, 230]`
  - `gravity: 0.045`
- `Error` 节点尺寸更大，普通节点也比之前更大。
- 主画布使用宽松的内边距，节点不会贴边。
- 右侧节点详情独立成栏，避免挤占图谱画布。

## 故障知识库页小图修正

原问题：

- “组件分布走势”看起来像不完整坐标图。
- 标签排列拥挤。
- 曲线不够自然。

调整后：

- 只展示 Top 5 组件。
- 用 SVG path 绘制平滑曲线。
- 使用淡蓝面积填充。
- 标签压缩为一行，减少视觉噪声。

## 本次知识图谱页显示修复

根据页面截图，本次修复集中在知识图谱页的展示稳定性：

- 根因：`loadGraph` / `searchGraph` 在 `graphLoading=true` 时就调用 `renderGraph()`，但模板中图谱画布依赖 `!graphLoading` 才显示，导致 ECharts 在隐藏容器或尺寸未稳定时初始化，图谱容易偏到画布左侧并被裁切。
- 修复：图谱数据返回后先结束 loading，再等待 `nextTick` 和一帧浏览器布局后渲染 ECharts。
- 修复：如果画布第一帧仍未获得有效宽高，会再等待一帧；仍无尺寸则跳过本次渲染，避免初始化出错。
- 修复：渲染前执行 `clear()` 和 `resize()`，并用 `setOption(..., true)` 避免沿用旧的 force 布局状态。
- 修复：增加 `ResizeObserver` 和窗口 resize 监听，容器尺寸变化时自动刷新图谱尺寸。
- 优化：明确知识图谱页 CSS Grid 中 hero、筛选区、关系图和右侧详情的行列位置，减少自动排版造成的空洞。
- 优化：关系图画布高度从 `680px` 提升到 `730px`，主卡片最小高度从 `760px` 提升到 `820px`。
- 优化：force layout 的节点斥力与边长进一步拉开：
  - `repulsion: 760`
  - `edgeLength: [175, 270]`
  - `gravity: 0.035`
- 优化：统计卡片略微收紧，避免过度拉高页面视觉重心。

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

知识图谱后端接口测试：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$repo='C:\Users\97780\.m2\repository'
..\tools\apache-maven-3.9.16\bin\mvn.cmd "-Dmaven.repo.local=$repo" -o -Dtest=KgControllerTest test
```

结果：

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 后续衔接

下一步建议做“分析历史页”。

原因：

- 智能排障接口已经会写入 `analysis_history`。
- 但当前后端还没有历史读取接口。
- 需要先补历史列表 / 详情接口，再做页面。

下一步可能新增接口：

- `GET /api/analysis-history/list`
- `GET /api/analysis-history/{id}`
- `DELETE /api/analysis-history/{id}`

页面建议：

- 管理员 / 维护员查看全部历史。
- 普通用户查看自己的历史。
- 列表展示输入摘要、技术栈、命中故障、AI 建议摘要、时间。
- 详情展示完整输入、图谱快照、AI 建议。
