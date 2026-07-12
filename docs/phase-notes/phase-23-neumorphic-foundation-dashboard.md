# 阶段 23：浮雕视觉基础与首页工作台

## 本阶段目标

在不修改登录页、注册页和业务接口的前提下，为登录后的系统建立统一的清冷白色浮雕视觉基础，并以首页作为后续页面改造的参考基准。

## 完成内容

1. 新增全局系统主题，统一冷白画布、表面层、文字、边框、状态色、凸起阴影和内凹阴影。
2. 调整 AppShell 的顶部栏、侧栏、按钮和工作区层级，保持固定的左上光源方向。
3. 首页改为非均匀工作台布局，包含工作区概览、七日分析趋势、解决率环、系统连接、LLM 服务、最近记录和故障类型分布。
4. 使用 Apache ECharts 5.6.0 绘制趋势柱线图、解决率仪表环和故障分布横向条形图。
5. 首页改为舒展型间距：桌面卡片间距 28px、主卡片内边距 30px，并允许工作区自然纵向滚动。
6. 删除卡片顶部贯穿式彩色线条，颜色只保留在图表、状态点、图标、关键数字和操作按钮中。
7. 修复故障类型图表空白：为图表提供明确高度，并以 `shallowRef` 保存 ECharts 实例，避免 Vue 深层代理破坏图表内部坐标系状态。

## 新增或修改文件

- `frontend/src/styles/system-theme.css`
- `frontend/src/main.js`
- `frontend/src/layouts/AppShell.vue`
- `frontend/src/utils/chartTheme.js`
- `frontend/src/views/dashboard/DashboardView.vue`
- `frontend/src/views/dashboard/dashboard-view.css`
- `frontend/scripts/check-ui-contract.mjs`
- `frontend/package.json`
- `docs/superpowers/specs/2026-07-11-neumorphic-system-redesign-design.md`
- `docs/superpowers/plans/2026-07-11-neumorphic-foundation-dashboard.md`

## 验证结果

- `npm.cmd run test:ui`：通过。
- `npm.cmd run build`：通过。
- 浏览器桌面视口 1280x720：无横向溢出。
- 三个 ECharts 画布均非空，故障类型分布图绘图区为 362x300px。
- 页面刷新后未出现新的 ECharts 控制台错误。
- 登录页和注册页未在本阶段修改。

## 后续衔接

后续页面按照智能排障、分析历史、故障知识库、我的收藏、知识图谱、用户管理、角色设置、个人资料的顺序逐页调整。每页沿用本阶段的主题令牌、凸起/内凹材质和图表主题，不一次性批量覆盖业务页面。
