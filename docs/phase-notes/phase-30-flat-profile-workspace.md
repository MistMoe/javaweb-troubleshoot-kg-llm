# 阶段 30：平面个人资料账户工作区

## 本阶段目标

将原先只读、由多张卡片组成的个人资料页改成可真实编辑的账户工作区。该页面供 `ADMIN`、`MAINTAINER`、`USER` 三种角色共同使用，并刻意采用独立的平面冷白设计，不延续业务页面的浮雕卡片材质。

## 完成内容

### 1. 页面结构调整

- 个人资料页改为一个连续的双栏工作区。
- 左侧为身份栏，显示头像字标、显示名称、用户名、角色、账号状态、分析记录数、收藏案例数和经验投稿数。
- 左侧底部保留智能排障、我的收藏、故障知识库三个文字快捷入口。
- 右侧由“基本资料”“登录安全”“最近活动”三个连续章节组成，章节之间使用极细分隔线，不再拆成独立卡片。
- 桌面端使用 300px 身份栏；900px 以下自动改为上下结构；640px 以下表单切换为单列。

### 2. 基本资料真实保存

新增接口：

```text
PUT /api/auth/profile
```

允许当前登录用户修改自己的：

- 显示名称 `nickname`
- 邮箱 `email`

用户身份由 Bearer token 对应的服务端会话确定，接口不接收用户 ID，因此不能借此修改其他账号。用户名、账号状态和角色仍然只读。保存成功后，后端同步刷新当前会话资料；前端通过 `profile-updated` 事件更新 `App.vue` 中的 `currentUser`，顶部用户信息立即变化。

### 3. 修改密码

新增接口：

```text
PUT /api/auth/password
```

密码修改规则：

- 必须输入当前密码。
- 新密码至少 6 位。
- 新密码不能与当前密码相同。
- 前端要求两次新密码输入一致。
- 修改成功后清空所有密码输入框。

当前课程项目继续沿用原有明文密码字段，以兼容现有演示账号。本阶段没有进行全库密码哈希迁移；若系统后续用于真实部署，应将密码迁移为 BCrypt 等安全哈希。

## 后端文件

- `backend/src/main/java/com/mistmoe/troubleshoot/auth/ProfileUpdateRequest.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/auth/PasswordChangeRequest.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/auth/PasswordChangeResponse.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/auth/UserProfile.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/auth/AuthRepository.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/auth/AuthService.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/auth/AuthController.java`
- `backend/src/test/java/com/mistmoe/troubleshoot/auth/AuthControllerTest.java`

## 前端文件

- `frontend/src/views/profile/ProfileView.vue`
- `frontend/src/views/profile/profile-view.css`
- `frontend/src/App.vue`
- `frontend/scripts/check-ui-contract.mjs`

## 测试过程

本阶段按测试先行完成：

1. 先增加资料修改和密码修改测试。
2. 运行时资料接口返回 405、密码接口返回 404，确认测试确实命中缺失功能。
3. 实现接口后，认证模块 8 项测试全部通过。
4. 增加前端双栏工作区契约，先观察 `profile-workspace` 缺失导致测试失败。
5. 完成页面后 UI 契约通过。

最终验证结果：

```text
后端完整测试：31 tests，0 failures，0 errors
前端 UI 契约：通过
前端 Vite 生产构建：通过
```

构建仍会显示项目原有的大 chunk 提示和 VueUse 注释提示，不影响本阶段功能。

## 后续衔接

- 可增加头像上传接口，并把头像地址加入 `UserProfile`。
- 可增加邮箱验证码或邮箱唯一性校验。
- 若准备公开部署，应统一迁移密码哈希、增加会话过期和退出后 token 失效机制。
- 手工演示时应分别使用三个角色验证个人资料页；修改昵称后观察顶部用户区，修改密码后退出并使用新密码重新登录。
