# Phase 7 页面能力边界与开发顺序

## 本阶段目标

本阶段不实现新页面代码，只梳理登录后业务页面的开发边界：

- 每个页面可以复用哪些现有接口。
- 每个页面对应 MySQL / Neo4j 哪些数据。
- 哪些功能现在可以马上做真实数据。
- 哪些功能适合先做静态或半静态展示。
- 哪些功能需要补后端接口。
- 后续页面推荐开发顺序。

说明：老师提供的 LLM 账密只作为本地开发配置使用，本文档不记录密钥明文。后续接入时应使用环境变量或本地未提交配置文件保存。

## 当前后端能力总览

### 现有接口

| 模块 | 接口 | 当前用途 |
| --- | --- | --- |
| 健康检查 | `GET /api/health` | 判断后端是否运行 |
| 登录 | `POST /api/auth/login` | 用户名密码登录，返回用户和角色信息 |
| 当前用户 | `GET /api/auth/profile` | 根据 token 获取当前用户 |
| 故障知识库 | `GET /api/error/list` | 查询故障案例，支持 keyword / component / stage 过滤 |
| 故障知识库 | `POST /api/error/create` | 新增故障案例 |
| 故障知识库 | `PUT /api/error/{id}` | 编辑故障案例 |
| 故障知识库 | `DELETE /api/error/{id}` | 删除故障案例 |
| 智能排障 | `POST /api/error/analyze` | 根据输入报错匹配知识库、查询图谱、生成建议、写入历史 |
| 知识图谱 | `POST /api/kg/import` | 从 MySQL 故障案例导入 Neo4j |
| 知识图谱 | `GET /api/kg/graph/{errorId}` | 获取某个故障案例的图谱关系 |
| 知识图谱 | `GET /api/kg/search?keyword=...` | 搜索 Neo4j 节点 |

### 现有 MySQL 数据

| 表 | 作用 | 主要字段 |
| --- | --- | --- |
| `sys_user` | 用户账号 | `id`, `username`, `password`, `nickname`, `email`, `status` |
| `sys_role` | 角色 | `id`, `role_code`, `role_name`, `description` |
| `sys_user_role` | 用户角色关系 | `user_id`, `role_id` |
| `error_case` | 故障知识库核心表 | `error_name`, `stage`, `component`, `keyword`, `symptom`, `cause`, `solution`, `related_file`, `related_command`, `risk_tip` |
| `analysis_history` | 智能排障历史 | `user_id`, `input_text`, `tech_stack`, `matched_error_id`, `matched_error_name`, `graph_result`, `ai_advice`, `create_time` |

### 现有 Neo4j 数据结构

`POST /api/kg/import` 会从 `error_case` 导入以下节点和关系：

| 类型 | 内容 |
| --- | --- |
| 节点 | `Error`, `Stage`, `Component`, `Symptom`, `Cause`, `Solution`, `File`, `Command` |
| 关系 | `OCCURS_IN`, `HAS_SYMPTOM`, `MAY_CAUSED_BY`, `SOLVED_BY`, `RELATED_TO`, `CHECK_FILE`, `USE_COMMAND` |

### 当前 LLM 状态

后端已有 `LlmAdviceService`，可以在 `POST /api/error/analyze` 中生成 AI 建议，但当前配置是：

- `llm.enabled: false`
- 默认 provider 是 `deepseek`
- 使用通用 HTTP Chat Completions 请求格式

老师给的是文心一言 / 千帆 OAuth 方式，需要补一层正式 Provider 适配，不能只把 Key 填到现有 DeepSeek 配置里。

## 页面能力地图

### 1. 智能排障页

定位：系统核心页，用户输入报错、选择技术栈，系统返回匹配案例、原因、方案、图谱关系和 AI 建议。

可用接口：

- `POST /api/error/analyze`
- `GET /api/error/list`
- `GET /api/kg/graph/{errorId}`

对应数据：

- MySQL：`error_case`, `analysis_history`
- Neo4j：`Error` 及其关联的 `Stage / Component / Symptom / Cause / Solution / File / Command`
- LLM：当前可走兜底建议，接入千帆后可生成真实大模型建议

可以马上做真实的功能：

- 报错文本输入。
- 技术栈选择。
- 调用 `/api/error/analyze` 返回真实匹配结果。
- 展示故障名称、阶段、组件、现象、原因、解决方案、文件、命令。
- 展示该故障关联图谱。
- 分析完成后写入 `analysis_history`。

适合先静态或半静态的功能：

- “分析进度条”“可信度评分”“推荐下一步”可以先根据匹配结果前端计算。
- AI 建议在千帆接入前会使用知识库兜底文案。

需要补后端：

- 千帆 / 文心一言 Provider。
- 可选：独立的 `/api/llm/chat`，供全局 AI 助手使用。
- 可选：更精细的匹配评分字段，让前端展示可信度。

推荐状态：第一批开发，优先做真实。

### 2. 知识图谱页

定位：展示故障案例和知识节点之间的关系，用于解释排障路径。

可用接口：

- `POST /api/kg/import`
- `GET /api/kg/graph/{errorId}`
- `GET /api/kg/search?keyword=...`
- `GET /api/error/list`

对应数据：

- MySQL：`error_case`
- Neo4j：图谱节点和关系

可以马上做真实的功能：

- 从 MySQL 导入 Neo4j。
- 选择故障案例并查看关系图。
- 搜索图谱节点。
- 展示节点数量、关系数量。
- 展示节点类型图例。

适合先静态或半静态的功能：

- 节点详情右侧面板可以先展示 `name / category / relation count`。
- 图谱筛选条件可以先在前端过滤已有结果。

需要补后端：

- 图谱总览接口，例如节点总数、关系总数、各类型数量。
- 节点详情接口，例如 `/api/kg/node/{id}`。
- 按类型筛选、按关系深度查询的接口。

推荐状态：第一批开发，和智能排障页衔接紧密。

### 3. 故障知识库页

定位：维护 `error_case`，让系统有可分析、可导入图谱的基础知识。

可用接口：

- `GET /api/error/list`
- `POST /api/error/create`
- `PUT /api/error/{id}`
- `DELETE /api/error/{id}`
- `POST /api/kg/import`

对应数据：

- MySQL：`error_case`
- Neo4j：由 `error_case` 导入生成

可以马上做真实的功能：

- 列表查询。
- 按关键字、组件、阶段筛选。
- 新增故障案例。
- 编辑故障案例。
- 删除故障案例。
- 修改后提示重新导入知识图谱。

适合先静态或半静态的功能：

- 分类统计、热度、最近更新趋势可以先用前端从列表聚合。
- 批量导入、导出可以暂缓。

需要补后端：

- 分页接口，避免后续数据多了前端一次性拉太多。
- 批量导入 / 导出接口。
- 字典接口，例如阶段列表、组件列表。

推荐状态：第一批开发，先把真实 CRUD 做稳。

### 4. 分析历史页 / 我的历史页

定位：查看过去的排障输入、匹配结果、图谱快照和 AI 建议。

可用接口：

- 当前只有 `POST /api/error/analyze` 会写入 `analysis_history`
- 暂无历史读取接口

对应数据：

- MySQL：`analysis_history`
- 可关联 MySQL：`error_case`
- 可展示历史中的 `graph_result` JSON 和 `ai_advice`

可以马上做真实的功能：

- 只要用户完成一次智能排障，后端已经会写入历史。

适合先静态或半静态的功能：

- 在补接口前，页面只能用静态列表或前端临时缓存展示。

需要补后端：

- `GET /api/analysis-history/list`
- `GET /api/analysis-history/{id}`
- `DELETE /api/analysis-history/{id}`
- 支持 `userId`、角色权限、关键字、技术栈、时间范围筛选。
- 管理员看全部，普通用户只看自己的历史。

推荐状态：第二批开发，先补接口再做页面，避免做成假页面。

### 5. 用户管理页

定位：管理员维护用户账号、状态和角色绑定。

可用接口：

- 登录相关只有 `POST /api/auth/login`
- 当前没有用户管理 CRUD 接口

对应数据：

- MySQL：`sys_user`, `sys_role`, `sys_user_role`

可以马上做真实的功能：

- 登录和当前用户展示已经真实。

适合先静态或半静态的功能：

- 管理列表 UI 可以先静态设计，但不建议长期停留在静态，因为它和权限强相关。

需要补后端：

- `GET /api/users`
- `POST /api/users`
- `PUT /api/users/{id}`
- `PATCH /api/users/{id}/status`
- `DELETE /api/users/{id}`
- `PUT /api/users/{id}/roles`
- 密码初始化或重置接口。

推荐状态：第三批开发，等核心排障链路稳定后做。

### 6. 角色管理 / 系统设置页

定位：维护角色权限，或展示系统配置项。当前前端菜单更接近“角色管理”，用户之前也提到“系统设置”，后续可合并成管理员设置页。

可用接口：

- 当前没有角色 CRUD 接口。
- 登录时会读取 `sys_role` 和 `sys_user_role`。

对应数据：

- MySQL：`sys_role`, `sys_user_role`
- 配置项：`application.yml` 中的 MySQL、Neo4j、LLM 配置，但这些不适合直接暴露明文。

可以马上做真实的功能：

- 通过当前用户角色决定菜单权限已经真实。

适合先静态或半静态的功能：

- 权限说明、系统连接状态、LLM 配置状态可以先展示。
- 敏感配置只展示“已配置 / 未配置”，不展示密钥。

需要补后端：

- `GET /api/roles`
- `POST /api/roles`
- `PUT /api/roles/{id}`
- `DELETE /api/roles/{id}`
- `GET /api/system/status`
- `GET /api/system/config-safe`
- `PUT /api/system/llm-config`，仅本地或管理员可用，且不返回密钥明文。

推荐状态：第三批开发。权限和配置要谨慎，放在核心业务之后更稳。

## 首页工作台的数据真实化边界

首页已经完成视觉工作台，但当前多数指标是展示型数据。

可以马上接真实：

- 后端运行状态：`GET /api/health`
- 知识库数量：`GET /api/error/list` 前端统计
- 图谱导入状态：调用 `GET /api/kg/search` 或新增状态接口后判断

需要补后端：

- `GET /api/dashboard/summary`
- 返回今日分析次数、成功率、平均响应、知识库数量、图谱节点数、关系数、LLM 调用次数。
- 统计数据来源主要是 `analysis_history`、`error_case`、Neo4j 图谱查询和 LLM 调用日志。

建议：首页最后再做“完全真实化”，先保证各业务页可用。

## LLM 正式接入建议

老师给的账号属于课程临时凭据，后续老师会修改，因此只适合本地开发和演示。

推荐实现方式：

1. 保留现有 `LlmAdviceService` 的兜底逻辑，避免 LLM 失败时系统不可用。
2. 新增千帆 / 文心一言 Provider，和现有通用 HTTP Provider 分开。
3. 配置改为环境变量或本地配置：
   - `QIANFAN_ACCESS_KEY`
   - `QIANFAN_SECRET_KEY`
   - `QIANFAN_MODEL`
4. `application.yml` 只保留占位，不提交真实密钥。
5. `/api/error/analyze` 使用 LLM 生成正式排障建议。
6. 后续给全局 AI 助手补 `POST /api/llm/chat`，让它可以跨页面问答。

这样做的好处是：演示时能真接大模型，老师改密钥后也只需要换环境变量，不需要改代码。

## 推荐开发顺序

### 第 1 步：LLM 千帆接入

原因：智能排障页和全局 AI 助手都依赖它。先接好大模型，后面的页面会更像完整系统。

交付：

- 后端支持千帆 Provider。
- 本地通过老师凭据可生成真实中文排障建议。
- LLM 调用失败时仍回退到知识库建议。

### 第 2 步：智能排障页

原因：这是系统主功能，也是最适合展示“JavaWeb 智能排障”的页面。

交付：

- 输入报错。
- 返回匹配案例。
- 展示原因、方案、文件、命令、图谱、AI 建议。
- 自动写入历史。

### 第 3 步：故障知识库页

原因：知识库是智能排障和图谱的源头，CRUD 已经有接口，适合做真实功能。

交付：

- 真实列表、筛选、新增、编辑、删除。
- 修改后可重新导入图谱。

### 第 4 步：知识图谱页

原因：依赖知识库数据。等知识库稳定后，图谱展示会更完整。

交付：

- 导入图谱。
- 搜索节点。
- 按故障案例查看关系图。
- 节点详情侧栏先做轻量版。

### 第 5 步：分析历史页

原因：当前只有写入，没有读取接口，需要先补后端。

交付：

- 历史列表。
- 历史详情。
- 查看当次 AI 建议和图谱快照。
- 管理员 / 普通用户权限区分。

### 第 6 步：用户管理与角色管理

原因：属于管理能力，不影响排障主链路，但适合完善系统完整度。

交付：

- 用户列表、启停、角色绑定。
- 角色列表、角色说明、权限范围。
- 系统设置页只展示安全配置状态，不展示敏感密钥。

### 第 7 步：首页工作台真实统计与全局 AI 助手真实问答

原因：首页是汇总视图，应在各业务页数据稳定后再真实化；全局 AI 助手也需要等 LLM 接口稳定后接入。

交付：

- 首页统计真实化。
- 全局 AI 助手调用后端 LLM 聊天接口。
- 可根据当前页面上下文提供提示问题。

## 总结

当前最适合“马上做真”的页面是：

1. 智能排障页。
2. 故障知识库页。
3. 知识图谱页。

当前必须先补后端再做真的页面是：

1. 分析历史页。
2. 用户管理页。
3. 角色管理 / 系统设置页。

最推荐的下一步是先接好千帆 / 文心一言 LLM，再做智能排障页。这样主链路会从“知识库匹配”升级为“知识库 + 图谱 + 大模型建议”，演示效果和系统完整度都会更强。
