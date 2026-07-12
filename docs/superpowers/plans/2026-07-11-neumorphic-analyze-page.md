# 智能排障浮雕工作台实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不更改 `/api/error/analyze`、Word/PDF 导出和 Neo4j 图谱数据结构的前提下，把智能排障页改造成舒展、清晰的浮雕分析工作台。

**Architecture:** 保留 `AnalyzeView.vue` 的请求与导出函数，以 12 列 CSS Grid 重组输入、结果、图谱和 AI 建议四个职责区。页面复用全局 `surface-card`、`inset-well`、`metric-plate` 和按钮令牌；ECharts 实例使用 `shallowRef` 并接入 ResizeObserver。

**Tech Stack:** Vue 3、Element Plus、Apache ECharts 5.6.0、lucide-vue-next、scoped page CSS、Node 静态 UI 契约检查。

## Global Constraints

- 不修改登录页、注册页、字体与背景图。
- 不修改 Spring Boot、MySQL、Neo4j、DeepSeek 接口契约。
- 不修改 ADMIN、MAINTAINER、USER 的权限规则。
- 不使用大面积渐变、彩色整边、霓虹和鼠标跟随特效。
- 页面允许工作区纵向滚动，禁止横向溢出。
- 每个阶段必须写入 `docs/phase-notes`。

---

### Task 1: 智能排障页面契约

**Files:**
- Modify: `frontend/scripts/check-ui-contract.mjs`
- Test: `frontend/scripts/check-ui-contract.mjs`

**Interfaces:**
- Consumes: 现有 Node 静态契约检查。
- Produces: 对分析轨道、全局材质类、图谱浅引用和大画布的回归约束。

- [x] **Step 1: 写入失败契约**

要求 `AnalyzeView.vue` 包含 `analysis-track`、`surface-card`、`shallowRef(null)`，要求 CSS 包含 `grid-template-areas` 和 `min-height: 420px`。

- [x] **Step 2: 运行契约并确认失败**

Run: `npm.cmd run test:ui`

Expected: FAIL，提示智能排障页面缺少新契约。

### Task 2: 页面结构与浮雕材质

**Files:**
- Modify: `frontend/src/views/analyze/AnalyzeView.vue`
- Modify: `frontend/src/views/analyze/analyze-view.css`

**Interfaces:**
- Consumes: `surface-card`、`inset-well`、`metric-plate`、`action-button`。
- Produces: `input`、`result`、`graph`、`advice` 四个网格区域和三段 `analysis-track`。

- [x] **Step 1: 重组四区布局**

桌面端使用 12 列：输入 5 列、结果 7 列、图谱 7 列、建议 5 列；间距 28px，卡片内边距 30px。

- [x] **Step 2: 建立过程轨道**

轨道固定展示“知识库匹配 / 图谱检索 / DeepSeek 建议”，根据 `analyzing` 与 `result` 显示待命、处理中、完成三种状态，不增加接口调用。

- [x] **Step 3: 统一控件材质**

日志文本域与结果内容区使用内凹层；技术栈、样例、导出和主按钮使用既有主题色，不使用渐变；空状态保留清晰边界。

### Task 3: 图谱生命周期稳定性

**Files:**
- Modify: `frontend/src/views/analyze/AnalyzeView.vue`

**Interfaces:**
- Consumes: `createChartResizeObserver(element, onResize)`。
- Produces: 使用 `shallowRef` 的 ECharts 实例与卸载清理函数。

- [x] **Step 1: 改用浅引用并监听尺寸**

`chart` 使用 `shallowRef(null)`；图谱画布最小高度 420px；渲染后注册 ResizeObserver。

- [x] **Step 2: 清理图表资源**

组件卸载时断开观察器并 `dispose()`，避免切页后的重复实例与尺寸错误。

### Task 4: 验证与阶段记录

**Files:**
- Create: `docs/phase-notes/phase-24-neumorphic-analyze-page.md`

**Interfaces:**
- Consumes: 完成后的页面与测试脚本。
- Produces: 可供后续分析历史页复用的布局和验证记录。

- [x] **Step 1: 运行自动验证**

Run: `npm.cmd run test:ui`

Expected: PASS。

Run: `npm.cmd run build`

Expected: exit 0，仅允许已有第三方 PURE 注释和 chunk 体积警告。

- [x] **Step 2: 浏览器验收**

检查桌面端四区顺序、无横向滚动、空状态和真实分析结果；确认图谱画布非空、AI 助手未遮挡主要操作。

- [x] **Step 3: 记录阶段结果**

写明修改文件、保持不变的接口、验证命令、浏览器结果和下一页衔接。
