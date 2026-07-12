# 阶段 17：报告导出与 ECharts 图表替换

## 本阶段完成内容

本阶段按演示需求补充了两个能力：排障报告导出，以及关键图表改为 ECharts 展示。

1. 在智能排障页的“分析结果”区域增加 Word / PDF 导出入口。完成一次智能排障后，用户可以把当前报错输入、命中结果、可能原因、解决方案、相关文件、建议命令、知识图谱证据和 AI 建议整理成报告。
2. 在分析历史页的“复盘详情”区域增加 Word / PDF 导出入口。选择一条历史记录后，可以导出该次排障的复盘报告，内容包含原始报错、命中状态、命中案例、图谱节点关系摘要、AI 建议和提交信息。
3. 新增前端通用导出工具 `frontend/src/utils/reportExport.js`。Word 导出采用浏览器端生成 `.doc` 文件；PDF 导出采用浏览器打印窗口，演示时可选择“另存为 PDF”。
4. 首页“今日排障状态”圆环由 CSS 静态环替换为 ECharts gauge 图表，保留原有清冷浮雕视觉。
5. 故障知识库“组件分布走势”由手写 SVG 折线替换为 ECharts 平滑折线图，减少点位拥挤和曲线不自然的问题。

## 涉及文件

- `frontend/src/utils/reportExport.js`
- `frontend/src/views/analyze/AnalyzeView.vue`
- `frontend/src/views/analyze/analyze-view.css`
- `frontend/src/views/history/HistoryView.vue`
- `frontend/src/views/history/history-view.css`
- `frontend/src/views/dashboard/DashboardView.vue`
- `frontend/src/views/dashboard/dashboard-view.css`
- `frontend/src/views/knowledge/KnowledgeBaseView.vue`
- `frontend/src/views/knowledge/knowledge-base-view.css`

## 演示衔接说明

演示智能排障时，可以先输入报错并生成分析结果，然后点击结果区右上角的 Word 或 PDF。Word 会直接下载，PDF 会打开浏览器打印窗口，选择“另存为 PDF”即可。

演示分析历史时，先选择左侧历史记录，再在右侧复盘详情点击 Word 或 PDF。这个入口更适合展示“系统沉淀排障过程”的能力。

首页和故障知识库页的图表现在都使用 ECharts，可在汇报中说明系统已经引入 Apache ECharts 用于可视化展示排障状态和知识库分布。

## 后续可扩展方向

当前导出为前端演示版，优点是实现轻、演示稳定、无需新增后端依赖。后续如果需要更正式的报告归档，可以在后端补充 Apache POI 生成 `.docx`，或引入 PDF 渲染库生成服务器端 PDF。

ECharts 后续可以继续扩展到分析历史页的趋势统计、知识图谱页的关系图，以及用户管理页的活跃度统计，但本阶段按要求只替换首页圆环和故障知识库折线图。

## 验证结果

已执行前端构建：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

构建通过。Vite 输出了已有的 VueUse 注释提示和 chunk 体积提示，不影响页面运行。
