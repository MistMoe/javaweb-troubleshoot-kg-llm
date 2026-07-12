# Phase 14 全局 AI 助手交互修复

## 本步目标

修复全局 AI 助手“看得见但用不了”的问题。

## 问题表现

- 快捷问题按钮点击后没有任何反应。
- 输入框无法形成对话。
- 发送按钮没有实际逻辑。
- 面板整体只是静态占位，不像可用的助手。

## 根因

`GlobalAiAssistant.vue` 之前只完成了视觉面板：

- 快捷问题按钮没有 `@click`；
- 输入框没有 `v-model`；
- 发送按钮没有发送函数；
- 没有消息列表状态；
- 没有根据用户输入生成回复的逻辑。

## 修复内容

修改文件：

- `frontend/src/components/global/GlobalAiAssistant.vue`

新增能力：

- 快捷问题可点击；
- 点击快捷问题后自动发送；
- 输入框支持输入问题；
- 回车可发送；
- 发送按钮可发送；
- 空输入时发送按钮禁用；
- 对话区会显示用户消息和 AI 回复；
- 发送后自动滚动到底部；
- 当前阶段使用静态规则回复，后续可接入真实 LLM 接口。

## 当前静态回复规则

根据关键词返回不同提示：

- `Spring Boot` / `spring`：返回启动失败排查链路；
- `图谱` / `KG` / `Neo4j`：返回知识图谱关系解释；
- `最近` / `历史`：返回分析历史页说明；
- `报告` / `report`：返回排障报告结构建议；
- 其他输入：返回通用排障建议。

## 后续衔接

后续如果要接真实大模型，可以把当前 `buildAssistantReply` 替换为后端接口调用，例如：

```http
POST /api/assistant/chat
```

请求内容可以包含：

- 用户输入；
- 当前页面；
- 当前登录用户；
- 最近一次分析记录；
- 当前图谱节点。

## 验证情况

前端构建通过：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

构建结果：

```text
vite build success
3927 modules transformed
```
