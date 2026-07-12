# Phase 6 - 首页工作台清冷白色浮雕优化

## 本步目标

在不修改登录页、注册页、字体和登录背景图的前提下，优化登录后的首页工作台。

本步聚焦首页视觉和结构：

- 从普通后台 Dashboard 卡片堆叠，改为清冷白色浮雕工作台。
- 首页改为 6 个组件的非均匀拼贴布局。
- 快捷入口融入具体组件，不再单独成排。
- AI 助手从右侧固定面板改为右下角浮标。

## 修改文件

- `frontend/src/views/dashboard/DashboardView.vue`
- `frontend/src/views/dashboard/dashboard-view.css`
- `frontend/src/layouts/AppShell.vue`

未修改：

- `frontend/src/LoginView.vue`
- `frontend/src/RegisterView.vue`
- `frontend/src/auth-view.css`
- `frontend/src/assets/login-hero-bg.png`

## 首页 6 个组件

### 1. Workspace Overview

顶部横向主面板，展示：

- WORKSPACE
- 欢迎回来，系统管理员 / 当前用户昵称
- 系统运行说明
- 今日智能排障分析数量
- 知识图谱状态

右侧使用低对比状态胶囊：

- System Online
- Neo4j Connected
- LLM Ready

### 2. 今日排障状态环

核心大组件，使用 CSS 绘制浅色环形进度，不引入 ECharts。

展示：

- 今日排障状态
- 32 次分析
- 85% 解决成功率
- 平均响应 2.4s

底部入口：

- `开始排障 ->`，跳转到智能排障菜单。

### 3. 系统连接状态

轻浮雕状态列表，展示：

- Neo4j Connected
- MySQL Connected
- Backend Running
- Frontend Ready

每项使用低饱和小圆点标识，不使用高饱和色块。

### 4. LLM 模型服务状态

展示大模型接入状态：

- 当前模型：DeepSeek / 文心一言
- 接口状态：Ready
- 今日调用：18
- 平均延迟：1.6s
- 模型服务正常运行

### 5. 最近分析记录

中型轻量列表，不再使用重表格。

示例记录：

- NullPointerException / Spring Boot / 已解决
- SQLSyntaxErrorException / MyBatis / 已解决
- Connection timeout / MySQL / 处理中
- Neo4j Unauthorized / Neo4j / 已解决

右上入口：

- `查看全部 ->`，根据当前角色跳转分析历史或我的历史。

### 6. 故障类型分布

中型组件，使用 CSS 横向进度条展示：

- Spring Boot 38%
- MySQL 24%
- Vue 18%
- Neo4j 12%
- LLM API 8%

底部入口：

- `进入知识库 ->`，跳转故障知识库。

## AI 助手浮标

AI 助手不再占用首页固定右侧面板。

改为右下角悬浮胶囊按钮：

- 按钮文字：AI 助手
- 图标：Sparkles
- 白色轻浮雕材质
- 淡蓝冷光边缘

点击后展开右下角聊天窗口，当前为静态占位：

- 标题：AI 助手
- 文案：我可以帮你分析报错、解释图谱关系、生成排障报告。
- 输入框占位：输入你的问题...
- 快捷问题：
  - 如何分析 Spring Boot 启动失败？
  - 解释知识图谱关系
  - 生成排障报告

## AppShell tooltip 修复

左侧导航 tooltip 曾可能被主内容层遮挡。

本步在 `AppShell.vue` 中调整：

- `shell-root` 增加 `isolation: isolate`
- `top-bar` 设置更高层级
- `side-rail` 设置 `position: relative; z-index: 35; overflow: visible`
- `rail-item span` 提升到 `z-index: 120`
- `workspace` 明确为较低层级

这样左侧导航悬浮提示可以覆盖主内容区域。

## 首页样式裸奔修复

用户实测时出现首页内容退回浏览器默认样式的问题，表现为：

- 6 个组件没有拼贴布局；
- 卡片浮雕样式未生效；
- 按钮显示为默认浏览器按钮；
- 列表和文本挤在一起。

排查后判断为首页组件在热更新过程中 scoped 样式没有正确进入页面。为降低 HMR 和缓存影响，本步将首页关键样式拆为独立文件：

- `frontend/src/views/dashboard/dashboard-view.css`

并在 `DashboardView.vue` 中显式导入：

```js
import './dashboard-view.css'
```

这样 Vite 会把首页样式作为普通模块依赖加载，避免组件模板更新后样式没有随之注入。

## 卡片悬停效果调整

用户反馈浮雕卡片在鼠标悬停时仍然会上浮，视觉上会回到普通后台卡片交互。

本步将首页卡片悬停效果从位移上浮改为不移动的冷光描边：

- 移除 `transform: translateY(-1px)`。
- 移除 `transition: transform ...`。
- 保留卡片原位置不动。
- hover 时只轻微提升边框冷光、冷白背景和内侧高光。

调整文件：

- `frontend/src/views/dashboard/DashboardView.vue`
- `frontend/src/views/dashboard/dashboard-view.css`

这种交互更贴合当前清冷白色浮雕风格：不是“卡片弹起来”，而是“边缘被轻轻点亮”。

## 验证

已执行：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

结果：构建通过。

说明：构建仍保留已有的大包体积提示，主要来自当前项目已有的 Element Plus / ECharts 等依赖，不影响本步首页运行。
