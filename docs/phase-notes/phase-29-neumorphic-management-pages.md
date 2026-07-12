# 阶段 29：用户管理与角色管理浮雕优化

## 本阶段目标

将用户管理和角色管理统一为管理型标题结构：左侧标题大卡，右侧一张汇总卡，汇总卡内部放三项紧凑指标。

## 用户管理

1. 标题卡与右侧汇总卡统一为 204px 高度。
2. 汇总卡内部保留账号总数、启用账号、系统角色三项指标，分别使用蓝、紫、青数字点色。
3. 筛选区改为全宽第二行，保留关键词、角色、状态和查询功能。
4. 刷新为白色浮雕次操作，新增用户为蓝色主操作。
5. 第三行保留左侧账号名册与右侧角色边界。
6. 用户条目使用轻抬浮雕，角色边界条目使用指标板材质，当前逻辑提示改为内凹区域。
7. 新增用户、编辑用户、角色分配和启停账号逻辑保持不变。

## 角色管理

1. 标题卡与右侧汇总卡统一为 204px 高度。
2. 汇总卡内部保留系统角色、菜单节点、权限层级三项指标，分别使用蓝、紫、青数字点色。
3. 保留角色说明、权限矩阵、登录展示流程和设计边界四个业务区。
4. 移除主体卡片彩色顶边和旧卡片阴影，统一使用系统冷白浮雕材质。
5. 角色条目与登录流程使用轻抬指标板，权限矩阵行使用内凹区域。
6. 固定三角色、菜单权限矩阵和角色加载逻辑保持不变。

## 修改文件

- `frontend/src/views/users/UserManagementView.vue`
- `frontend/src/views/users/user-management-view.css`
- `frontend/src/views/roles/RoleManagementView.vue`
- `frontend/src/views/roles/role-management-view.css`
- `frontend/scripts/check-ui-contract.mjs`
- `docs/superpowers/specs/2026-07-11-management-pages-neumorphic-design.md`
- `docs/superpowers/plans/2026-07-11-management-pages-neumorphic-plan.md`

## 保持不变

- `GET /api/users/list`
- `GET /api/users/roles`
- 用户创建、更新和启停接口
- ADMIN / MAINTAINER / USER 角色与菜单可见性
- 用户编辑弹窗字段与角色选择逻辑
- 登录注册、全局导航和 AI 助手

## 验证结果

- `npm.cmd run test:ui`：通过。
- `npm.cmd run build`：通过。
- 构建仅保留第三方 PURE 注释和大体积 chunk 的既有提示。

## 同行卡片对齐修复

- 根据页面截图确认，用户管理的账号名册与角色边界、角色管理的两组左右卡片底边不齐。
- 根因是 `.users-grid` 和 `.role-grid` 使用 `align-items: start`，导致同一网格行内的卡片按各自内容高度收缩。
- 两个页面的桌面网格均改为 `align-items: stretch`，同一行左右卡片现在共享行高并自动对齐。
- 该修复不写死业务卡片高度，后续用户数量或权限行变化时仍能保持对齐。

## 后续衔接

下一页建议优化个人资料。个人资料属于账户型页面，可继续使用管理型材质，但标题右侧应展示当前账号身份与资料完整度，不需要复制三指标汇总卡。
