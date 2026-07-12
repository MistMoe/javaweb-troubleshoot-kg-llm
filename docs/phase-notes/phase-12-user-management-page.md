# Phase 12 用户管理页与分析历史布局修正

## 本步目标

本阶段做两件事：

1. 修正分析历史页的排版错位，让历史记录更靠上，并把技术栈分布放到历史记录右侧。
2. 开始用户管理页，补齐用户列表、角色列表、新增、编辑、启停的前后端闭环。

登录页、注册页、首页整体布局、全局 AI 助手均未重做。

## 分析历史页调整

- 调整 `HistoryView` 的 CSS Grid 区域：
  - 顶部 hero 高度收紧；
  - 筛选栏紧跟 hero；
  - 历史记录位于左侧主区域；
  - 技术栈分布位于历史记录右侧；
  - 复盘详情位于右侧下方。
- 减少空态高度，避免接口暂无数据时出现大面积空白。
- 增加蓝色边框、蓝色状态点、蓝色进度条等元素。
- 去掉历史页中的渐变渲染，改为克制的纯色蓝色点缀。

## 用户管理后端

新增后端接口：

```http
GET  /api/users/list?keyword=&roleCode=&status=
GET  /api/users/roles
POST /api/users/create
PUT  /api/users/{id}
PUT  /api/users/{id}/status
```

对应数据表：

- `sys_user`
- `sys_role`
- `sys_user_role`

后端支持：

- 查询用户及其角色；
- 查询系统角色；
- 新增用户；
- 编辑用户基础信息和角色；
- 启用 / 停用用户；
- 校验用户名、密码、状态值和角色是否存在。

## 用户管理前端

新增页面：

- `frontend/src/views/users/UserManagementView.vue`
- `frontend/src/views/users/user-management-view.css`

页面结构：

- 顶部用户管理说明区；
- 筛选区：关键词、角色、状态；
- 统计区：账号总数、启用账号、系统角色；
- 账号名册：展示用户、邮箱、角色、启停状态；
- 角色边界说明：解释 ADMIN / MAINTAINER / USER 的菜单权限；
- 新增 / 编辑用户弹窗。

页面已挂载到 `AppShell` 中的 `users` 菜单，仅 ADMIN 可见。

## 后续衔接

下一步可以继续做：

1. 角色管理页：展示角色、角色说明、菜单权限矩阵。
2. 系统设置页：展示 MySQL、Neo4j、LLM、前端代理等运行配置。
3. 将用户管理页面和登录权限进一步联动，例如禁止停用当前登录账号。

## 验证情况

- 前端构建已通过：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

- 后端用户管理测试已经先写 RED 测试，但本次 Maven 执行被当前 Codex 权限 / 额度系统拦截，未能完成 GREEN 验证。额度恢复后建议运行：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$repo='C:\Users\97780\.m2\repository'
..\tools\apache-maven-3.9.16\bin\mvn.cmd "-Dmaven.repo.local=$repo" -o -Dtest=UserAdminControllerTest test
```

## 返修记录

### 2026-07-09 分析历史页布局重叠

问题表现：

- 分析历史页右侧的“复盘详情”和“技术栈分布”在空态下发生视觉重叠。

原因：

- 原先直接用 `grid-row` 让左侧历史记录跨两行，右侧两个卡片分别占第 3 / 第 4 行。空态高度变化时，CSS Grid 自动行计算不稳定，导致右侧卡片视觉上压到一起。

修复：

- 将历史页改为明确的 `grid-template-areas`：
  - `hero`
  - `filters`
  - `timeline`
  - `spectrum`
  - `detail`
- 让右侧“技术栈分布”和“复盘详情”按命名区域稳定排列。

### 2026-07-09 用户管理新增用户后端报错

问题表现：

- `UserAdminControllerTest.createAndUpdateUserWithRoles` 报错：

```text
The getKey method should only be used when a single key is returned.
The current key entry contains multiple keys: [{id=4, create_time=..., update_time=...}]
```

原因：

- H2 测试库插入 `sys_user` 后返回了多个 generated keys：`id`、`create_time`、`update_time`。
- 原代码使用 `keyHolder.getKey()`，该方法只适合数据库只返回一个 key 的场景。

修复：

- 新增 `extractGeneratedId`，优先从 generated keys map 中读取 `id` / `ID`。
- 若只返回一个单值 key，则兼容单值读取。
- 无法读取时抛出明确异常。

验证：

```text
UserAdminControllerTest: Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
frontend npm.cmd run build: success
```
