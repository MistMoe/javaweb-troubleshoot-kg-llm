# 分析历史浮雕复盘台实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将分析历史页整理为左侧历史时间线、右侧技术栈分布与复盘详情的舒展型浮雕复盘台。

**Architecture:** 保留历史列表、详情、删除和导出逻辑，将主体改为 `history-workspace` 两列结构。页面复用全局浮雕材质；长文本在详情内部滚动，不引入新接口或新图表依赖。

**Tech Stack:** Vue 3、Element Plus、lucide-vue-next、CSS Grid、Node 静态 UI 契约检查。

## Global Constraints

- 不修改 `/api/analysis-history` 接口契约。
- 不修改个人历史 `userId` 过滤逻辑、删除逻辑和 Word/PDF 导出。
- 不修改登录注册页与 AppShell 菜单权限。
- 不使用渐变、彩色整边和无业务含义图表。
- 页面禁止横向溢出，并兼容 900px 窄屏。

---

### Task 1: 页面契约

**Files:**
- Modify: `frontend/scripts/check-ui-contract.mjs`

- [x] 写入 `history-workspace`、`history-side`、`surface-card`、`max-height: 460px` 和 28px 间距契约。
- [x] 运行 `npm.cmd run test:ui`，确认因新结构缺失而失败。

### Task 2: 页面结构与材质

**Files:**
- Modify: `frontend/src/views/history/HistoryView.vue`
- Modify: `frontend/src/views/history/history-view.css`

- [x] 压缩页头并将三个统计改为横向 `metric-plate`。
- [x] 用 `history-workspace` 包裹左侧时间线和右侧 `history-side`。
- [x] 右侧按技术栈分布、复盘详情顺序排列。
- [x] 使用全局 `surface-card`、`inset-well` 和 `action-button`，删除渐变和整边强调。
- [x] 为 AI 建议设置 460px 最大高度和内部滚动。

### Task 3: 验证与记录

**Files:**
- Create: `docs/phase-notes/phase-25-neumorphic-history-page.md`

- [x] 运行 UI 契约与生产构建。
- [x] 浏览器验证真实历史加载并定位全局类名冲突；最终修复后的视觉复核由用户刷新确认。
- [ ] 在 900x800 验证单栏响应式。
- [x] 写阶段记录并注明下一页为故障知识库。
