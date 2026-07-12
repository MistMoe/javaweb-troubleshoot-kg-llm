# 阶段 28：分析历史高度统一与知识图谱浮雕工作区

## 本阶段目标

统一分析历史标题卡高度，并在保留知识图谱独立四卡头部的前提下优化工具区、关系画布和节点详情。

## 分析历史调整

- `.history-hero` 最小高度由 154px 调整为 204px。
- 现在与故障知识库、我的收藏的标题区域保持相同高度基准。
- 历史统计、筛选、列表、详情与 Word/PDF 导出逻辑没有变化。

## 知识图谱调整

1. 顶部保留左侧知识图谱标题大卡，右侧保留节点、关系、类型三张独立统计卡。
2. 四张卡统一为 204px 高度、相同冷白浮雕材质和上下边界。
3. 节点、关系、类型数字分别使用蓝、紫、青小面积点色，不使用彩色整边。
4. 故障案例、节点搜索、查看关系、搜索节点、刷新和 MySQL 导入集中到第二行全宽控制带。
5. “从 MySQL 导入图谱”为蓝色主按钮，刷新改为图标按钮，其余查询按钮使用白色浮雕次操作。
6. 第三行保留左侧大尺寸关系画布和右侧节点详情，画布高度仍为 730px，满足图谱舒展展示。
7. 画布、加载态、空状态、节点详情和使用提示统一使用内凹浮雕材质，移除旧页面渐变背景。
8. 图谱实例改用 `shallowRef`，画布类改为 `kg-graph-canvas`，避免 Vue 深层代理和跨页面 `.graph-canvas` 污染。

## 保持不变

- `GET /api/error/list`
- `POST /api/kg/import`
- `GET /api/kg/graph/{errorId}`
- `GET /api/kg/search?keyword=...`
- ECharts 力导向布局、缩放、拖动、节点点击和邻接高亮
- ResizeObserver 与窗口尺寸变化处理
- Neo4j 数据、角色权限、导航和 AI 助手
- 登录页与注册页

## 修改文件

- `frontend/src/views/history/history-view.css`
- `frontend/src/views/graph/KnowledgeGraphView.vue`
- `frontend/src/views/graph/knowledge-graph-view.css`
- `frontend/scripts/check-ui-contract.mjs`
- `docs/superpowers/specs/2026-07-11-knowledge-graph-neumorphic-design.md`
- `docs/superpowers/plans/2026-07-11-knowledge-graph-neumorphic-plan.md`

## 验证结果

- `npm.cmd run test:ui`：通过。
- `npm.cmd run build`：通过。
- 构建保留 Vite 对第三方 PURE 注释和大体积 chunk 的既有警告，不影响页面编译。

## 后续衔接

下一页建议优化用户管理。用户管理使用“管理型标题”：标题区右侧放核心账号数据和主要管理操作，不照搬知识图谱的三张独立统计卡。
