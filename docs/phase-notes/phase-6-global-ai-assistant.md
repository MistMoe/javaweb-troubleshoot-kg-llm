# Phase 6 - 全局 AI 助手优化

## 本步目标

在不修改登录页、注册页、首页主体布局和字体的前提下，将 AI 助手从首页局部元素升级为登录后的全局能力。

用户反馈的问题：

- 右下角 AI 助手入口不够明显；
- 展开后的悬浮窗太小；
- AI 助手只像首页局部元素；
- 需要所有登录后的业务页面都能使用 AI 助手。

## 修改文件

- `frontend/src/components/global/GlobalAiAssistant.vue`
- `frontend/src/layouts/AppShell.vue`
- `frontend/src/views/dashboard/DashboardView.vue`
- `frontend/src/views/dashboard/dashboard-view.css`

未修改：

- 登录页；
- 注册页；
- 登录页字体；
- 登录页背景图；
- 首页 6 个主体组件的整体布局。

## 实现方式

### 1. 新增全局组件

新增：

- `frontend/src/components/global/GlobalAiAssistant.vue`

组件内部负责：

- AI 助手入口按钮；
- 展开 / 收起状态；
- 大尺寸聊天窗口；
- 欢迎消息；
- 静态示例消息；
- 快捷问题；
- 底部输入框。

当前仍为静态交互，不接真实 LLM 接口。

### 2. 挂载到 AppShell

在：

- `frontend/src/layouts/AppShell.vue`

统一引入：

```js
import GlobalAiAssistant from '../components/global/GlobalAiAssistant.vue'
```

并挂载在 `AppShell` 根布局内：

```vue
<GlobalAiAssistant />
```

因此只要用户登录后进入 `AppShell`，AI 助手就会显示。

登录页和注册页没有使用 `AppShell`，所以不会显示 AI 助手。

### 3. 从首页移除局部 AI 助手

在：

- `frontend/src/views/dashboard/DashboardView.vue`

移除了旧的首页局部 AI 助手浮标。

在：

- `frontend/src/views/dashboard/dashboard-view.css`

清理了旧的 `.ai-dock`、`.assistant-trigger`、`.assistant-window` 等首页局部样式。

这样首页只负责 6 个工作台组件，全局 AI 助手由 `AppShell` 统一提供。

## 入口按钮设计

AI 助手入口固定在右下角：

- `right: 28px`
- `bottom: 28px`
- `position: fixed`

按钮尺寸：

- 高度：`56px`
- 最小宽度：`158px`
- 圆角：`24px`

视觉结构：

- 左侧 `Sparkles` 图标；
- 中间文字 `AI 助手`；
- 右侧小状态点。

风格：

- 清冷白色浮雕胶囊；
- 淡蓝边缘高光；
- 低对比柔和阴影；
- hover 时轻微提亮并上浮 1px。

## 展开窗口设计

展开后的窗口尺寸：

- 宽度：`min(460px, calc(100vw - 36px))`
- 高度：`min(620px, calc(100vh - 124px))`

桌面端约为 `460px × 620px`，比原来的小提示框更接近可用的智能助手。

窗口结构：

1. Header 顶部栏
   - 标题：AI 助手
   - 副标题：智能排障与知识图谱助手
   - 最小化按钮
   - 关闭按钮

2. 对话内容区
   - 欢迎语
   - 静态示例消息
   - 空状态提示

3. 快捷问题区
   - 分析 Spring Boot 启动失败
   - 解释图谱关系
   - 查看最近排障记录
   - 生成排障报告

4. 输入区
   - placeholder：请输入报错信息或你的问题...
   - 发送按钮

## 层级与可见性

AI 助手根节点使用：

```css
position: fixed;
z-index: 3000;
pointer-events: none;
```

按钮和窗口单独恢复：

```css
pointer-events: auto;
```

这样可以保证：

- 页面滚动时 AI 助手固定在右下角；
- 不被普通卡片、表格、主内容区遮挡；
- 层级高于 `AppShell` 左侧导航 tooltip；
- 不阻挡页面其他区域点击。

## 后续衔接

后续如果接入真实 LLM，可以在 `GlobalAiAssistant.vue` 内继续扩展：

- 增加消息数组；
- 增加发送状态；
- 调用后端 LLM 分析接口；
- 支持将当前页面上下文带入问题，例如当前故障案例、当前图谱节点、当前分析结果。

由于组件已经挂在 `AppShell`，后续所有业务页都可以共享这一入口。

## 验证

## 蓝色入口微调

用户反馈白色 AI 助手入口和首页白色浮雕材质太接近，不够明显。

本次微调：

- 将入口从白色浮雕胶囊改为低饱和冰蓝色胶囊；
- 文字保持明确的 `AI 助手`；
- 位置从右下边缘略微向左上移动；
- 桌面端位置：`right: 48px; bottom: 56px`;
- 按钮尺寸：高度 `58px`，最小宽度 `172px`，圆角 `25px`;
- 保留淡蓝外圈高光和柔和阴影，不使用高饱和广告蓝；
- 展开窗口结构和首页主体布局未改动。

已执行：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

结果：构建通过。

构建仍有历史大包体积提示，主要来自已有 Element Plus / ECharts 等依赖，不影响本步功能。
