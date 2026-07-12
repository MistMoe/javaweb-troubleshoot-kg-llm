# Neumorphic Foundation and Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立登录后系统统一的冰晶浮雕设计基础，并将 AppShell 与工作台首页改造成第一张视觉基准页。

**Architecture:** 在 `system-theme.css` 中集中维护设计令牌和公共材质类，AppShell 只负责全局框架，Dashboard 只负责首页网格和数据展示。ECharts 公共颜色、轴线和 tooltip 配置通过 `chartTheme.js` 复用，不修改接口或页面路由。

**Tech Stack:** Vue 3、Vite 5、Element Plus 2、lucide-vue-next、Apache ECharts 5.5.1、CSS Custom Properties。

## Global Constraints

- 不修改 `LoginView.vue`、`RegisterView.vue`、`auth-view.css` 和登录背景图。
- 不修改后端接口、请求参数、角色权限和菜单顺序。
- 页面不使用大面积渐变，不使用星芒、霓虹或均匀环形灰影。
- 页面底色为浅蓝灰，面板为冷白；图表使用蓝、紫、青、绿，珊瑚红只用于异常。
- 桌面端页面区块间距为 `24px`，卡片圆角不超过 `8px`。
- 悬停不整体位移，仅增强高光、边缘色与阴影清晰度。
- Git 元数据当前不可用；每个任务使用构建结果、阶段文档和已验证的完整目录备份作为检查点。

---

### Task 1: 添加前端 UI 契约检查

**Files:**
- Create: `frontend/scripts/check-ui-contract.mjs`
- Modify: `frontend/package.json`

**Interfaces:**
- Consumes: Node.js 标准库 `fs`、当前 Vue/CSS 源文件。
- Produces: `npm run test:ui`，检查全局令牌、AppShell 类名、Dashboard 图表与登录页保护边界。

- [ ] **Step 1: 创建会失败的静态契约检查**

```js
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const read = path => readFileSync(resolve(root, path), 'utf8')
const theme = read('src/styles/system-theme.css')
const shell = read('src/layouts/AppShell.vue')
const dashboard = read('src/views/dashboard/DashboardView.vue')

const required = [
  [theme, '--surface-canvas'],
  [theme, '.surface-card'],
  [theme, '.inset-well'],
  [shell, 'shell-surface'],
  [dashboard, 'trendChartRef'],
  [dashboard, 'distributionChartRef']
]

for (const [content, token] of required) {
  if (!content.includes(token)) throw new Error(`Missing UI contract: ${token}`)
}

console.log('UI contract check passed')
```

- [ ] **Step 2: 添加 npm 脚本并确认检查失败**

在 `frontend/package.json` 的 `scripts` 中添加：

```json
"test:ui": "node scripts/check-ui-contract.mjs"
```

Run: `npm.cmd run test:ui`

Expected: FAIL，因为 `system-theme.css` 和首页新图表引用尚未创建。

### Task 2: 建立全局设计令牌与公共材质

**Files:**
- Create: `frontend/src/styles/system-theme.css`
- Modify: `frontend/src/main.js`
- Test: `frontend/scripts/check-ui-contract.mjs`

**Interfaces:**
- Consumes: 无业务数据。
- Produces: `--surface-*`、`--color-*`、`--shadow-*` 设计令牌，以及 `.surface-card`、`.surface-card--strong`、`.inset-well`、`.metric-plate`、`.status-chip` 公共类。

- [ ] **Step 1: 定义固定光源的设计令牌**

```css
:root {
  --surface-canvas: #e5ecf3;
  --surface-soft: #edf3f8;
  --surface-panel: #f6f9fc;
  --surface-highlight: #ffffff;
  --text-primary: #17263d;
  --text-secondary: #6f8299;
  --text-muted: #93a3b5;
  --line-soft: rgba(139, 164, 191, 0.24);
  --accent-blue: #4f86e8;
  --accent-violet: #9689e6;
  --accent-cyan: #62b9ca;
  --accent-mint: #68b99a;
  --accent-coral: #dc7378;
  --shadow-raised: -8px -8px 20px rgba(255, 255, 255, 0.82), 12px 14px 28px rgba(114, 139, 166, 0.18);
  --shadow-raised-soft: -5px -5px 14px rgba(255, 255, 255, 0.72), 8px 10px 20px rgba(114, 139, 166, 0.13);
  --shadow-inset: inset 3px 3px 8px rgba(129, 151, 174, 0.14), inset -3px -3px 8px rgba(255, 255, 255, 0.78);
}
```

- [ ] **Step 2: 定义公共材质类**

`.surface-card` 使用面板底色、极细边框、`8px` 圆角和 `--shadow-raised-soft`；`.surface-card--strong` 使用 `--shadow-raised`；`.inset-well` 使用 `--shadow-inset`。所有 hover 只改变边框和阴影，不使用 `transform`。

- [ ] **Step 3: 在入口文件中导入主题**

在 `frontend/src/main.js` 的 `./styles.css` 后加入：

```js
import './styles/system-theme.css'
```

- [ ] **Step 4: 运行基础检查**

Run: `npm.cmd run test:ui`

Expected: 仍然 FAIL，但错误推进到缺少 `shell-surface` 或首页图表引用。

### Task 3: 改造 AppShell

**Files:**
- Modify: `frontend/src/layouts/AppShell.vue`
- Test: `frontend/scripts/check-ui-contract.mjs`

**Interfaces:**
- Consumes: 原有 `currentUser`、`primaryRole`、`visibleMenus`、`activeMenu` props 与三个 emit。
- Produces: 相同组件接口；增加 `shell-surface`、`rail-well` 等样式钩子，不改变父组件调用方式。

- [ ] **Step 1: 添加稳定样式钩子**

为顶部栏添加 `shell-surface`，侧栏添加 `rail-well`，主工作区保留 `.workspace`。不改菜单循环、事件与图标。

- [ ] **Step 2: 将框架改为固定光源浮雕**

具体要求：顶部栏高度从 `62px` 压缩到 `58px`；背景使用 `var(--surface-soft)`；侧栏保持 `72px`；工作区使用 `24px` padding；图标按钮使用外凸材质，active 状态转为蓝色内凹；Tooltip 的 `z-index` 保持高于主内容。

- [ ] **Step 3: 检查登录后导航契约**

Run: `npm.cmd run test:ui`

Expected: 仅因 Dashboard 新图表引用缺失而 FAIL。

- [ ] **Step 4: 浏览器验证框架**

验证顶部栏、侧栏、Tooltip、个人资料入口、退出按钮、全局 AI 助手均可见且不被裁切；窗口宽度 `1280px` 和 `900px` 无横向滚动。

### Task 4: 建立 ECharts 公共主题

**Files:**
- Create: `frontend/src/utils/chartTheme.js`
- Modify: `frontend/src/views/dashboard/DashboardView.vue`

**Interfaces:**
- Produces: `chartPalette`、`chartAxisStyle`、`chartTooltipStyle`、`createResizeObserver(chartRef)`。
- Consumes: Dashboard 现有 ECharts 实例。

- [ ] **Step 1: 添加图表公共配置**

```js
export const chartPalette = ['#4f86e8', '#9689e6', '#62b9ca', '#68b99a', '#dc7378']

export const chartAxisStyle = {
  axisLine: { lineStyle: { color: 'rgba(139, 164, 191, 0.34)' } },
  axisTick: { show: false },
  axisLabel: { color: '#7f91a6', fontSize: 11 },
  splitLine: { lineStyle: { color: 'rgba(139, 164, 191, 0.16)' } }
}

export const chartTooltipStyle = {
  backgroundColor: 'rgba(246, 249, 252, 0.98)',
  borderColor: 'rgba(139, 164, 191, 0.34)',
  borderWidth: 1,
  textStyle: { color: '#17263d' }
}
```

- [ ] **Step 2: Dashboard 导入配置**

```js
import { chartAxisStyle, chartPalette, chartTooltipStyle } from '../../utils/chartTheme'
```

- [ ] **Step 3: 保留环形状态图并统一配色**

将仪表进度色改为 `chartPalette[1]`，轨道改为透明蓝灰；取消强烈发光，只保留轻微阴影。

### Task 5: 改造首页模板与图表

**Files:**
- Modify: `frontend/src/views/dashboard/DashboardView.vue`
- Modify: `frontend/src/views/dashboard/dashboard-view.css`
- Test: `frontend/scripts/check-ui-contract.mjs`

**Interfaces:**
- Consumes: 当前用户、导航 emit、首页现有演示数据。
- Produces: `trendChartRef`、`distributionChartRef` 两个 ECharts 容器，保持 `navigate` emit 不变。

- [ ] **Step 1: 压缩欢迎区**

保留 `WORKSPACE`、欢迎语和运行说明，将欢迎区最小高度降到约 `112px`。右侧状态改为三项横向或两行数据铭牌，不再纵向堆叠三个胶囊。

- [ ] **Step 2: 重排非均匀网格**

桌面端使用十二列布局：趋势主面板占七列，连接与模型组合占五列；第二行历史记录占七列，故障分布占五列。区块间距统一为 `24px`。

- [ ] **Step 3: 添加趋势组合图**

新增 `trendChartRef`，使用一条蓝色折线与蓝紫青三组低饱和柱形，展示最近七日分析量与解决率。图表背景透明，坐标轴使用公共配置，不使用渐变填充。

- [ ] **Step 4: 添加故障类型分布图**

新增 `distributionChartRef`，使用水平条形图展示 Spring Boot、MySQL、Vue、Neo4j、LLM API；每项使用固定语义色，保留底部“进入知识库”入口。

- [ ] **Step 5: 完成浮雕与内凹层级**

外层 tile 使用 `.surface-card`；趋势与分布画布使用 `.inset-well`；状态列表使用浅内凹条目；禁止卡片 hover 位移；删除原有大面积渐变和重复线条纹理。

- [ ] **Step 6: 验证 UI 契约由红转绿**

Run: `npm.cmd run test:ui`

Expected: PASS，输出 `UI contract check passed`。

### Task 6: 构建、视觉回归与阶段记录

**Files:**
- Create: `docs/phase-notes/phase-23-neumorphic-foundation-dashboard.md`

**Interfaces:**
- Consumes: Task 1 至 Task 5 的结果。
- Produces: 可回顾的完成说明和下一页衔接事项。

- [ ] **Step 1: 完整构建**

Run: `npm.cmd run build`

Expected: Vite 构建成功，退出码为 `0`；允许保留当前依赖注释与大 chunk 警告，不允许编译错误。

- [ ] **Step 2: 浏览器桌面验收**

在 `http://localhost:5173/` 验证：登录注册页未改变；首页无横向滚动；顶部栏、侧栏、Tooltip、AI 助手层级正常；两张新增图表非空且 resize 正常。

- [ ] **Step 3: 浏览器窄屏验收**

在 `900px` 和 `720px` 宽度检查：网格降为单列或双列；文字不溢出；按钮可点击；AI 助手不遮挡主要操作。

- [ ] **Step 4: 写阶段文档**

记录修改文件、公共设计令牌、首页图表、验证命令、已知警告和后续业务页面接入方式。
