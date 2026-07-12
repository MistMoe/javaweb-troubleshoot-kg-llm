# Phase 6 - AppShell 与首页工作台

## 本步目标

在不修改登录/注册页视觉、不改后端逻辑、不接入新接口的前提下，先完成登录后的整体页面骨架和首页工作台。

本步只做两件事：

- 新增浅蓝灰智能工作台风格的登录后全局布局 `AppShell`。
- 新增首页 / 工作台 `DashboardView`，用静态展示先确定整体视觉和信息结构。

## 已完成内容

### 1. AppShell 全局布局

新增文件：

- `frontend/src/layouts/AppShell.vue`

布局结构：

- 顶部 `TopBar`：包含菜单图标、系统名称、当前页面名称、搜索图标、通知图标、角色、用户头像、退出按钮。
- 左侧 `SideRail`：窄导航栏，只展示 lucide 线性图标，悬停时显示菜单名称。
- 主工作区 `workspace`：登录后的页面内容统一渲染在这里，外层不滚动，内容区内部滚动。

视觉风格：

- 浅蓝灰背景。
- 半透明浅色面板。
- 细线边框。
- 低对比文字。
- 统一使用 `lucide-vue-next` 线性图标。

### 2. 首页工作台 Dashboard

新增文件：

- `frontend/src/views/dashboard/DashboardView.vue`

首页模块：

- 欢迎区。
- 四个数据概览卡片：今日分析、解决成功率、知识图谱节点、活跃用户。
- 四个快捷入口：智能排障、知识图谱、故障知识库、分析历史。
- 最近分析记录静态列表。
- 右侧 AI 助手面板。
- 右侧知识图谱概览面板。

首页只使用静态数据，不请求后端接口，方便先确认整体页面风格。

### 3. App.vue 接入方式

修改文件：

- `frontend/src/App.vue`

调整内容：

- 登录页与注册页仍然保持原组件，不做视觉改动。
- 登录成功后改为渲染新的 `AppShell`。
- `activeMenu === 'home'` 时显示 `DashboardView`。
- 其他菜单暂时显示浅色占位页，后续再逐个迁移业务页面。
- 原来的深色后台布局没有删除，只是禁用保留，避免误删已有业务逻辑。

### 4. 依赖变化

修改文件：

- `frontend/package.json`
- `frontend/package-lock.json`

新增依赖：

- `lucide-vue-next`

说明：安装时 npm 提示该包已 deprecated，建议使用 `@lucide/vue`。本步仍按当前需求使用 `lucide-vue-next`，因为用户明确要求统一使用它。

## 后续衔接

下一步可以在不重做 AppShell 的前提下，逐页迁移业务模块：

1. 智能排障页：把旧的报错输入、分析结果、图谱关系结果迁入新工作台风格。
2. 知识图谱页：重做图谱工具区、筛选区、右侧节点详情。
3. 故障知识库页：重做列表、筛选、编辑弹窗视觉。
4. 分析历史页：补历史记录列表和详情页。
5. 用户 / 角色管理页：按演示需求决定是否细化。

迁移时建议继续复用：

- `frontend/src/layouts/AppShell.vue`
- `activeMenu`
- `selectMenu`
- 现有 `App.vue` 中已经写好的业务函数和状态

## 验证

已执行：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

结果：构建通过。

构建时仍有历史体积警告，主要来自当前项目已引入的 Element Plus / ECharts 等大依赖，不影响本步页面运行。
