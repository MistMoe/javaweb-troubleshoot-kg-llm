# 阶段 25：分析历史浮雕复盘台

## 本阶段目标

保持历史接口、个人数据范围、删除与 Word/PDF 导出不变，将分析历史页整理为左侧历史记录与技术栈分布、右侧复盘详情的稳定工作区。

## 完成内容

1. 页头压缩为横向概览，记录数、命中率和图谱节点使用蓝、紫、青三组数据铭牌。
2. 筛选条紧接页头，关键词、命中状态和刷新操作保持原有功能。
3. 新增 `history-workspace` 两列主体：左侧 `history-primary-column` 依次放置历史记录和技术栈分布，右侧 `history-side` 单独放置复盘详情，避免右侧内容过长、左右视觉失衡。
4. 历史选中项使用蓝色内凹状态，技术栈使用蓝、紫、青、绿和珊瑚红的小面积数据色，不使用渐变与彩色整边。
5. 原始输入最大高度 230px，AI 建议最大高度 460px，长内容在详情内部滚动。
6. 结构类改为 `history-filter-card`、`history-timeline-card`、`history-spectrum-card` 和 `history-detail-card`，修复知识库、收藏页等全局 `.filter-card`、`.detail-card` 对本页布局的污染。

## 修改文件

- `frontend/src/views/history/HistoryView.vue`
- `frontend/src/views/history/history-view.css`
- `frontend/scripts/check-ui-contract.mjs`
- `docs/superpowers/plans/2026-07-11-neumorphic-history-page.md`

## 保持不变

- `GET /api/analysis-history/list`
- `GET /api/analysis-history/{id}`
- `DELETE /api/analysis-history/{id}`
- USER 的 `userId` 个人历史过滤逻辑
- Word/PDF 导出逻辑
- 登录注册页、菜单与角色权限

## 验证结果

- 浏览器实际加载 8 条历史记录、100% 命中率和 71 个图谱节点，详情与 DeepSeek 建议数据正常。
- 浏览器验收发现旧全局类名导致右侧列压缩，已用页面专属结构类从根因隔离。
- 修复后 UI 契约检查通过，生产构建通过。
- 最终修复后的浏览器视觉复核因本地浏览器权限中断未自动执行，需要用户刷新页面确认右栏宽度。
- 900x800 最终响应式复核未自动执行；CSS 在 1080px 断点改为单栏，生产构建已验证语法有效。

## 后续衔接

下一页为故障知识库。改造时优先为通用结构类增加页面前缀，避免 `.filter-card`、`.detail-card` 等旧全局选择器再次交叉污染。
