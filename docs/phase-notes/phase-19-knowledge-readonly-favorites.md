# Phase 19：知识库浏览权限与故障案例收藏

## 本阶段目标

本阶段补齐普通用户侧的知识库使用闭环：普通用户可以进入故障知识库浏览案例并收藏，维护员和管理员继续负责编辑、删除、同步 Neo4j、审核投稿等知识维护动作。

## 完成内容

1. 调整知识库页面权限
   - `USER` 角色现在可以在登录后菜单中看到“故障知识库”。
   - 角色说明页同步更新：知识库页面不再只属于 `ADMIN` 和 `MAINTAINER`。

2. 按角色隐藏维护按钮
   - `ADMIN`、`MAINTAINER`：保留新增案例、编辑、删除、同步 Neo4j、审核投稿。
   - `USER`：隐藏新增、编辑、删除、同步、审核，仅保留浏览、搜索、投稿经验和收藏。

3. 新增故障案例收藏能力
   - 新增 MySQL 表 `error_case_favorite`，用 `user_id + error_case_id` 唯一约束避免重复收藏。
   - 新增后端接口：
     - `GET /api/favorites/cases?userId=`
     - `GET /api/favorites/case-ids?userId=`
     - `POST /api/favorites/cases/{caseId}?userId=`
     - `DELETE /api/favorites/cases/{caseId}?userId=`
   - 前端知识库列表增加“收藏 / 已收藏”按钮。
   - 案例详情中增加“收藏此案例 / 已加入收藏”按钮。

4. 修复投稿审核区域被挤下去的问题
   - 审核面板改为左侧内容流展示，不再与右侧案例详情跨行区域争抢整行空间。
   - 右侧详情卡继续保持固定信息辅助区，页面结构更稳定。

## 后续衔接

后续要做“故障案例收藏页面”时，可以直接复用 `/api/favorites/cases?userId=` 接口读取当前用户收藏的案例列表，不需要重新设计收藏数据结构。

后续要做“个人资料页面”时，可以在用户资料中展示收藏数量、投稿数量、分析次数等个人统计，其中收藏数量可由 `error_case_favorite` 按 `user_id` 统计得到。
