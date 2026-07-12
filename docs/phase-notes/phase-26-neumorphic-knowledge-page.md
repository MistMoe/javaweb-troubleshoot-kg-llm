# 阶段 26：故障知识库浮雕界面优化

## 本阶段目标

保留故障知识库现有的大卡片布局和全部业务逻辑，只优化卡片内部结构、留白、操作层级、案例列表、投稿审核与组件分布图的视觉表现。

## 完成内容

1. 保留“头部与统计、筛选、案例列表、右侧案例详情”的原有大区块位置，将区块间距调整为 24px，使页面更舒展。
2. 卡片统一复用系统级 `surface-card`，移除知识库原有的大面积渐变和重复阴影定义。
3. 三项统计只在数字上使用蓝、紫、青小面积点色，不再给整张卡片添加彩色边线。
4. 筛选输入框改为内凹浮雕控件；案例行默认轻抬，选中后使用内压状态，收藏、编辑和删除仍按角色权限显示。
5. 右侧组件分布图与详情正文使用 `inset-well`，让图表、报错现象、原因、方案和文件命令形成清晰的内容层级。
6. 投稿审核的列表与编辑详情也改为内凹工作区，保留 ADMIN / MAINTAINER 审核、通过入库和驳回流程。
7. 将主要结构类改为 `knowledge-*` 页面专属类，避免 `.filter-card`、`.detail-card` 和 `.case-row` 污染其他页面。
8. ECharts 实例改用 `shallowRef` 保存，避免 Vue 对图表实例进行深层响应式代理。

## 修改文件

- `frontend/src/views/knowledge/KnowledgeBaseView.vue`
- `frontend/src/views/knowledge/knowledge-base-view.css`
- `frontend/scripts/check-ui-contract.mjs`
- `docs/superpowers/plans/2026-07-11-neumorphic-knowledge-page.md`
- `docs/phase-notes/phase-25-neumorphic-history-page.md`

## 保持不变

- 故障案例查询、新增、编辑和删除接口
- 用户收藏与取消收藏接口
- 经验投稿、DeepSeek 候选字段生成、投稿审核与入库流程
- ADMIN / MAINTAINER / USER 的权限分工
- MySQL 数据来源和 Neo4j 同步逻辑
- 登录页、注册页、字体和背景图

## 验证结果

- `npm.cmd run test:ui`：通过。
- `npm.cmd run build`：通过。
- 构建仅保留 Vite 对大体积 chunk 和第三方 PURE 注释的既有警告，不影响本次页面编译。
- 当前环境未自动执行浏览器视觉复核；需要在运行中的页面刷新后确认实际窗口下的留白与右侧详情高度。

## 后续衔接

下一页继续沿用系统浮雕基底和页面专属类名。若改造知识图谱页，应优先保证画布尺寸、节点展开距离和右侧节点详情稳定，不改变 Neo4j 查询与交互逻辑。
