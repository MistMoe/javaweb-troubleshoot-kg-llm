# 阶段 27：数据型标题体系与我的收藏优化

## 本阶段目标

建立“统一骨架、保留页面个性”的标题体系，并完成故障知识库的小修与我的收藏页面优化。

## 故障知识库

1. 保留原有页面特征：左侧标题大卡，右侧案例总数、技术组件、发生环节三张独立小卡。
2. 四张卡统一为 204px 高度，使用相同的冷白浮雕材质和上下边界。
3. 三项统计分别使用蓝、紫、青数字点色，卡片本身不使用彩色整边。
4. 投稿经验、待审核、刷新、同步 Neo4j 和新增案例移入独立操作带。
5. 新增案例为蓝色主操作；投稿与同步为白色次操作；待审核保留状态感；刷新改为图标按钮。
6. 筛选、投稿审核、案例列表、收藏、编辑删除、案例详情与 ECharts 图表逻辑保持不变。

## 我的收藏

1. 标题区改为合并式数据总览卡，右侧显示收藏案例、技术组件和常用组件。
2. 新增独立操作带，刷新使用图标按钮，“去知识库”为蓝色主入口。
3. 主体仍为左侧收藏列表、右侧收藏详情，筛选与取消收藏逻辑保持不变。
4. 空状态高度由 300px 降至 220px，减少大面积空白。
5. 收藏行默认使用轻抬浮雕，选中后使用内压状态；详情信息使用内凹内容块。
6. 收藏页通用选择器全部限制在 `.favorites-view` 内，避免影响其他页面。

## 修改文件

- `frontend/src/views/knowledge/KnowledgeBaseView.vue`
- `frontend/src/views/knowledge/knowledge-base-view.css`
- `frontend/src/views/favorites/FavoriteCasesView.vue`
- `frontend/src/views/favorites/favorite-cases-view.css`
- `frontend/scripts/check-ui-contract.mjs`
- `docs/superpowers/specs/2026-07-11-page-heading-variants-favorites-design.md`
- `docs/superpowers/plans/2026-07-11-knowledge-favorites-layout-plan.md`

## 保持不变

- 登录页和注册页
- 收藏查询、取消收藏和知识库跳转
- 故障案例、投稿审核、角色权限与 Neo4j 同步接口
- 全局导航与 AI 助手

## 后续衔接

后续页面继续使用三种标题变体：数据型、管理型和工作型。用户管理与角色设置使用管理型标题，不照搬数据型页面的三项统计布局。
