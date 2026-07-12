# Phase 16 全局 AI 助手接入后端聊天接口

## 本步目标

修复右下角全局 AI 助手“看起来像 AI，但实际只是前端静态回复”的问题，让它真正请求后端接口，并在 DeepSeek 启用时调用真实大模型。

## 问题表现

用户输入“你好”等内容后，助手几乎瞬间返回固定话术：

```text
收到。我建议先把报错现象、技术栈、关键日志和你已经尝试过的操作整理出来...
```

这说明它没有走后端，也没有走 DeepSeek。

## 根因

DeepSeek 启动脚本只会影响后端 LLM 配置。

但全局 AI 助手组件 `GlobalAiAssistant.vue` 之前仍然使用前端本地函数 `buildAssistantReply()` 生成回复，没有调用任何接口。

所以即使后端已经通过 DeepSeek 脚本启动，全局 AI 助手也不会自动接入真实模型。

## 修改内容

新增后端文件：

- `backend/src/main/java/com/mistmoe/troubleshoot/assistant/AssistantController.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/assistant/AssistantChatRequest.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/assistant/AssistantChatResponse.java`
- `backend/src/test/java/com/mistmoe/troubleshoot/assistant/AssistantControllerTest.java`

修改后端文件：

- `backend/src/main/java/com/mistmoe/troubleshoot/llm/LlmAdviceService.java`

修改前端文件：

- `frontend/src/components/global/GlobalAiAssistant.vue`

修改文档：

- `README.md`

## 新增接口

```http
POST /api/assistant/chat
```

请求示例：

```json
{
  "message": "怎么分析 Spring Boot 启动失败？"
}
```

响应示例：

```json
{
  "reply": "先检查端口、配置文件和依赖冲突。",
  "source": "llm"
}
```

## 后端逻辑

`AssistantController` 负责接收前端问题，并调用：

```java
llmAdviceService.generateAssistantReply(message)
```

`LlmAdviceService` 新增了全局助手专用方法：

- 如果 `llm.enabled=false`，返回本地兜底回复；
- 如果 provider 是 `deepseek`，走 OpenAI-compatible chat completions；
- 如果 provider 是 `qianfan`，走千帆 / 文心一言接口；
- 如果真实模型调用失败，返回兜底回复并提示 LLM 调用失败。

## 前端逻辑

全局 AI 助手不再直接把 `buildAssistantReply()` 当作主要回复来源。

现在发送消息后会：

1. 添加用户消息；
2. 显示“正在请求后端 AI 助手...”；
3. 请求 `/api/assistant/chat`；
4. 用后端返回的 `reply` 更新助手消息；
5. 如果接口失败，再使用本地兜底回复。

## 使用方式

如果要让全局 AI 助手真正调用 DeepSeek，需要用 DeepSeek 脚本启动后端：

```powershell
cd D:\javaweb-troubleshoot-kg-llm
.\scripts\start-backend-deepseek.ps1
```

然后正常启动前端：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run dev
```

## 验证情况

后端新增测试通过：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
..\tools\apache-maven-3.9.16\bin\mvn.cmd "-Dmaven.repo.local=C:\Users\97780\.m2\repository" -Dtest=AssistantControllerTest test
```

结果：

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

前端构建通过：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

结果：

```text
vite build success
3927 modules transformed
```

## 后续衔接

后续可以继续增强：

- 在 AI 助手窗口显示回复来源，例如“DeepSeek 生成”或“本地兜底”；
- 把当前路由、当前选中的故障案例或图谱节点一起传给后端；
- 让 AI 助手支持查看最近分析历史、生成完整排障报告；
- 增加流式输出，减少大模型回复等待时的停顿感。

