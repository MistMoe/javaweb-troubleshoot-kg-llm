# Phase 20：个人资料页与故障案例收藏页

## 本阶段目标

在不重做系统整体视觉、不改登录注册页的前提下，补齐登录后所有角色都可使用的个人能力页面：

- 侧边栏新增“我的收藏”；
- 右上角用户信息区进入“个人资料”；
- `ADMIN`、`MAINTAINER`、`USER` 三种角色都可以访问这两个页面。

## 完成内容

1. 我的收藏页
   - 新增 `frontend/src/views/favorites/FavoriteCasesView.vue`。
   - 新增 `frontend/src/views/favorites/favorite-cases-view.css`。
   - 页面读取 `/api/favorites/cases?userId=`，展示当前账号收藏的故障案例。
   - 支持关键词过滤、查看收藏详情、取消收藏、跳转故障知识库。
   - 空状态会引导用户去知识库收藏案例。

2. 个人资料页
   - 新增 `frontend/src/views/profile/ProfileView.vue`。
   - 新增 `frontend/src/views/profile/profile-view.css`。
   - 展示当前账号用户名、昵称、邮箱、账号状态和角色说明。
   - 读取个人分析历史、收藏案例、投稿记录，形成个人统计和最近活动。
   - 提供快捷入口：智能排障、我的收藏、故障知识库。

3. 全局入口
   - 修改 `frontend/src/App.vue`，新增 `favorites` 和 `profile` 两个页面分发。
   - `favorites` 放入侧边栏菜单，三个角色都可见。
   - `profile` 作为隐藏菜单项，不占侧边栏，通过右上角用户区进入。
   - 修改 `frontend/src/layouts/AppShell.vue`，右上角用户名和头像现在可以点击进入个人资料。

4. 角色说明同步
   - 修改 `frontend/src/views/roles/RoleManagementView.vue`。
   - 权限矩阵补充“我的收藏”和“个人资料”，三种角色均可访问。

## 后续衔接

如果后续要继续增强个人中心，可以优先补：

- 修改昵称和邮箱；
- 修改密码；
- 收藏数量、投稿数量的后端聚合接口；
- 头像上传；
- 从收藏页直接定位到知识库对应案例。

当前版本已经满足演示闭环：普通用户可以浏览知识库、收藏案例，并在“我的收藏”中管理自己的速查案例；所有角色都可以进入个人资料页查看账号与个人数据概况。
