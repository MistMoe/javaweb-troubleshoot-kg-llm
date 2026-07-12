# 阶段 24：智能排障浮雕工作台

## 本阶段目标

在保持分析接口、MySQL 案例匹配、Neo4j 图谱检索、DeepSeek 建议和 Word/PDF 导出不变的前提下，把智能排障页改造成与首页一致但更偏分析流程的舒展型浮雕工作台。

## 完成内容

1. 页面改为 12 列非均匀布局：输入区 5 列、结果区 7 列，图谱区 7 列、AI 建议区 5 列。
2. 卡片间距统一为 28px，主卡片内边距统一为 30px；桌面端保留双栏，1180px 以下转为单栏。
3. 新增“知识库匹配 / 图谱检索 / DeepSeek 建议”三段分析轨道，展示等待、分析中和完成状态。
4. 日志文本域、结果空状态、原因方案、证据、图谱和 AI 建议使用内凹层；主按钮、导出按钮、状态数字与图谱节点承担页面颜色。
5. ECharts 图谱实例改用 `shallowRef`，接入 ResizeObserver，并在组件卸载时断开观察器和释放实例。
6. 图谱画布使用页面专属类名 `analyze-graph-canvas`，修复与知识图谱页 `.graph-canvas` 全局样式冲突的问题。
7. 图谱画布固定为 420px；长 DeepSeek 回复限制为 560px 的内部滚动阅读区，避免第二行卡片被拉伸出大片空白。

## 修改文件

- `frontend/src/views/analyze/AnalyzeView.vue`
- `frontend/src/views/analyze/analyze-view.css`
- `frontend/scripts/check-ui-contract.mjs`
- `docs/superpowers/plans/2026-07-11-neumorphic-analyze-page.md`

## 保持不变

- `POST /api/error/analyze` 的请求参数与响应结构。
- MySQL 故障案例匹配和 Neo4j 图谱数据。
- DeepSeek 真实建议生成。
- Word 与 PDF 导出逻辑。
- ADMIN、MAINTAINER、USER 的页面权限。
- 登录页和注册页。

## 验证结果

- UI 契约检查通过。
- 前端生产构建通过。
- 浏览器使用预置“端口占用”样例完成真实分析，命中“端口被占用”，返回 8 个图谱节点、7 条关系和真实 DeepSeek 建议。
- 图谱画布尺寸为 566x420px，ECharts 控制台无错误。
- 长建议阅读区高度为 560px，可内部滚动；工作区总高度由约 4014px 收敛至约 1690px。
- 900x800 窄屏下四张卡片转为单栏，工作区 `scrollWidth` 与 `clientWidth` 均为 813px，无横向溢出。

## 后续衔接

下一页改造“分析历史”。沿用本阶段的分析轨道状态色、内凹详情面板和专属类名原则，重点解决历史列表、复盘详情和技术栈图表之间的层级关系。
