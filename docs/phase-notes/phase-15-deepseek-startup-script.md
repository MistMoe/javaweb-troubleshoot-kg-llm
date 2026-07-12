# Phase 15 DeepSeek 后端启动脚本

## 本步目标

减少每次启动真实 LLM 时需要手动输入的一长串 PowerShell 环境变量，让后端可以更方便地接入自己的 DeepSeek API Key。

## 背景

系统后端已经预留 OpenAI-compatible 的 LLM 调用逻辑，默认配置为 DeepSeek：

```yaml
llm:
  enabled: false
  provider: deepseek
  api-url: https://api.deepseek.com/chat/completions
  model: deepseek-chat
  api-key: ${LLM_API_KEY:}
```

之前如果要启用真实 DeepSeek，需要在启动后端前手动设置 `LLM_ENABLED`、`LLM_PROVIDER`、`LLM_API_URL`、`LLM_MODEL` 和 `LLM_API_KEY`。这一步容易漏，也不适合每次演示都重复输入。

## 新增文件

- `scripts/start-backend-deepseek.ps1`

## 脚本能力

脚本会自动完成：

- 检查后端目录是否存在；
- 检查 JDK 路径是否存在；
- 检查 Maven 命令是否存在；
- 设置 `JAVA_HOME`；
- 设置 DeepSeek 相关 LLM 环境变量；
- 启动 Spring Boot 后端；
- 退出后恢复脚本启动前的环境变量。

## API Key 处理方式

脚本不会把 DeepSeek API Key 写入文件。

如果当前 PowerShell 中已经存在 `LLM_API_KEY`，脚本会直接使用它。

如果不存在，脚本会提示输入：

```text
Enter DeepSeek API Key (input is hidden and will not be saved)
```

输入内容只作为当前后端进程的环境变量传给 Spring Boot。

## 使用方式

PowerShell：

```powershell
cd D:\javaweb-troubleshoot-kg-llm
.\scripts\start-backend-deepseek.ps1
```

前端仍然按原方式启动：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run dev
```

## 生效范围

该脚本主要影响智能排障页中的 AI 建议生成。

右下角全局 AI 助手目前仍是前端静态交互组件，后续如果要让它也调用 DeepSeek，需要新增后端聊天接口，例如：

```http
POST /api/assistant/chat
```

## 验证方式

启动后端后，到智能排障页提交一条报错。

如果 LLM 调用成功，建议内容会更接近模型生成的结构化中文分析。

如果仍然看到“当前使用知识库兜底建议”或“LLM 调用失败”，需要检查：

- DeepSeek API Key 是否正确；
- 当前网络是否可以访问 `https://api.deepseek.com/chat/completions`；
- 后端启动时是否使用了 `scripts/start-backend-deepseek.ps1`；
- DeepSeek 账户余额或模型权限是否正常。

## 后续衔接

后续可以继续做两件事：

- 给全局 AI 助手补后端真实聊天接口；
- 给前端智能排障页增加“当前建议来源”提示，例如“知识库兜底”或“DeepSeek 生成”。

