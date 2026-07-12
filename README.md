# JavaWeb Troubleshoot KG LLM 智能排障系统

## 最新补充：知识库经验投稿与审核

系统新增了“用户修复经验上传审核”能力，入口放在“故障知识库”页面。这个功能用于补充真实排障经验：用户可以粘贴报错日志、选择技术栈、填写发生环节、补充自己的修复过程、相关文件和排查命令。后端会调用 DeepSeek 将这些非结构化内容整理成候选知识库案例字段，包括故障名称、关键词、报错现象、可能原因、解决方案、相关文件、相关命令和风险提示。

投稿不会直接写入正式知识库，而是先进入 `error_case_submission` 待审核表。`ADMIN` 和 `MAINTAINER` 可以在故障知识库页打开“待审核”面板，查看投稿原文和 AI 生成的候选字段，并在前端继续编辑修正。审核通过后，系统会把候选案例写入已有 `error_case` 表，成为正式故障知识库案例；审核驳回时会保存驳回原因。通过后的案例仍然复用原来的“同步到 Neo4j”能力，因此新投稿沉淀为知识库后，也可以继续进入知识图谱。

这个功能和后续页面的衔接点已经保留：`submit_user_id` 可以给个人资料页展示“我的投稿”，`approved_error_case_id` 可以给故障案例收藏页关联正式案例，审核状态可以用于展示待审核、已通过、已驳回的贡献记录。

新增接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/submissions/draft` | 调用 DeepSeek 生成候选案例字段 |
| `POST` | `/api/submissions/create` | 提交排障经验，进入待审核 |
| `GET` | `/api/submissions/list` | 查询投稿列表，支持 `status`、`submitUserId` |
| `GET` | `/api/submissions/{id}` | 查看投稿详情 |
| `PUT` | `/api/submissions/{id}/approve` | `ADMIN` / `MAINTAINER` 审核通过并写入 `error_case` |
| `PUT` | `/api/submissions/{id}/reject` | `ADMIN` / `MAINTAINER` 驳回投稿 |

如果数据库已经初始化过，需要额外执行一次补表脚本：

```sql
source D:/javaweb-troubleshoot-kg-llm/docs/sql/2026-07-10-error-case-submission.sql;
```

如果重新初始化数据库，`database/mysql_schema.sql` 已经包含 `error_case_submission` 表结构。

## 近期完整能力补充

在知识投稿审核之外，系统已经补齐个人使用与演示闭环：三个角色都可以进入故障知识库浏览案例并收藏，收藏结果会写入 `error_case_favorite`，随后可在“我的收藏”页面集中检索、查看详情和取消收藏。智能排障和分析历史均支持导出 Word 报告或调用浏览器打印生成 PDF，便于课程提交、故障归档和现场演示。

个人资料页已经由静态展示改为真实账户工作区。当前登录用户可以修改自己的显示名称、邮箱和密码；用户名、角色和账号状态保持只读。资料保存后会同步刷新顶部用户信息，修改密码时必须验证原密码。首页排障趋势、故障类型分布和故障知识库组件分布使用 Apache ECharts 展示，不再依赖纯 CSS 假图表。

## 项目简介

JavaWeb Troubleshoot KG LLM 是一个面向 JavaWeb 开发与运维场景的智能排障系统。项目以 MySQL 作为结构化故障知识库，以 Neo4j 作为故障关系知识图谱，并接入 DeepSeek 大模型建议生成能力，用于把常见报错、技术栈、发生环节、可能原因、解决方案、配置文件和排查命令组织成可检索、可分析、可展示的排障闭环。

系统的核心思路不是只做一个普通后台管理页面，而是把“故障知识沉淀、图谱关系展示、报错智能分析、历史复盘、多角色管理”串成一个完整工作台。管理员和维护人员可以持续维护故障案例，普通用户可以提交报错并获得排障建议，系统会记录每一次分析过程，方便后续复盘、演示和扩展。

## 建设目标

- 将 JavaWeb 常见故障沉淀为可维护的知识库，而不是散落在文档或聊天记录中。
- 用 Neo4j 表达故障、组件、环节、现象、原因、方案、文件和命令之间的关系。
- 用户输入报错后，系统可以匹配知识库案例、检索图谱关系、生成排障建议，并保存分析历史。
- 通过多角色账号展示不同身份在系统中的功能边界。
- 用清冷白色浮雕风格做登录后工作台，避免普通后台系统的笨重感。
- 为课程汇报和后续 PPT 整理提供完整、可回顾的开发资料。

## 核心业务流程

1. 管理员或维护员在故障知识库中维护 JavaWeb 常见故障案例。
2. 故障案例保存到 MySQL 的 `error_case` 表。
3. 点击导入图谱后，系统从 MySQL 读取故障案例并写入 Neo4j。
4. 用户在智能排障页提交报错信息、技术栈和上下文。
5. 后端匹配 MySQL 知识库中的相似案例。
6. 后端检索 Neo4j 中与故障相关的节点和关系。
7. 系统结合匹配结果、图谱证据和 DeepSeek 大模型生成排障建议；未启用密钥时会自动使用本地兜底建议。
8. 分析结果写入 `analysis_history`，供分析历史页复盘。
9. 用户可以在分析结果或分析历史中导出 Word / PDF 排障报告。
10. 普通用户浏览并收藏故障案例，也可以投稿自己的修复经验。
11. 管理员或维护员审核投稿，通过后写入正式知识库并继续同步到 Neo4j。
12. 前端在知识图谱、分析历史、故障知识库、我的收藏和个人资料等页面展示完整闭环。

## 系统功能

系统围绕 JavaWeb 故障排查场景，已经实现了从账号登录、知识维护、智能分析、图谱展示到历史复盘的完整功能链路。

| 功能模块 | 主要能力 |
| --- | --- |
| 登录、注册与个人资料 | 支持演示账号登录、注册页展示；登录后可修改昵称、邮箱和密码，用户名、角色与状态保持只读。 |
| 多角色访问 | 支持 `ADMIN`、`MAINTAINER`、`USER` 三类角色，不同角色看到的页面入口不同。 |
| 首页工作台 | 展示系统运行状态、今日排障状态、系统连接状态、模型服务状态、最近分析记录和故障类型分布。 |
| 智能排障 | 用户输入报错、技术栈和上下文后，系统匹配故障知识库、检索 Neo4j 图谱，并结合 DeepSeek 生成排障建议；结果支持 Word / PDF 导出。 |
| 故障知识库 | 支持案例查询、收藏、投稿经验；管理员和维护员还可新增、编辑、删除、审核投稿并同步 Neo4j。 |
| Neo4j 知识图谱 | 支持从 MySQL 故障案例导入图谱，展示故障、组件、环节、原因、方案、文件、命令之间的关系。 |
| 图谱搜索与详情 | 支持按关键词搜索图谱节点，点击节点后查看节点类型、名称、ID 和相关连线信息。 |
| 分析历史 | 保存每次智能排障输入、命中案例、图谱证据和 AI 建议，支持筛选、详情复盘、删除及 Word / PDF 导出。 |
| 我的收藏 | 三个角色都可收藏知识库案例，并在独立页面检索、查看快照或取消收藏。 |
| 经验投稿与审核 | 用户提交报错日志和修复过程，DeepSeek 整理候选字段，管理员或维护员审核后沉淀到正式知识库。 |
| 用户管理 | 管理员可查看用户列表，按关键词、角色、状态筛选用户，并新增、编辑、启用或禁用用户。 |
| 角色管理 | 展示系统角色说明和权限矩阵，用于解释不同账号为什么看到不同功能入口。 |
| 全局 AI 助手 | 登录后所有业务页面都显示右下角 AI 助手，可输入问题或点击快捷问题，通过后端 `/api/assistant/chat` 调用 DeepSeek 回复。 |
| ECharts 可视化 | 首页展示排障趋势与故障分布，故障知识库展示组件分布趋势，知识图谱展示关系网络。 |
| 数据初始化与同步 | 提供 MySQL 初始化脚本、Neo4j 初始化脚本，并支持通过接口把 MySQL 故障案例同步到 Neo4j。 |
| 本地演示脚本 | 提供 `scripts/start-backend-deepseek.ps1`，启动后端时安全输入 DeepSeek API Key，不把密钥写入仓库。 |

从演示角度看，系统最核心的功能闭环是：

```text
维护故障知识库 -> 导入 Neo4j 知识图谱 -> 用户提交报错 -> 系统智能分析 -> 生成排障建议 -> 保存分析历史
```

## 技术栈

后端使用 Java 17 和 Spring Boot 3.3.5，主要包括 Spring Web、Spring JDBC、MySQL Connector/J、H2 测试数据库和 Maven。当前后端以清晰的 Controller、Service、Repository 分层实现业务逻辑，便于继续扩展权限控制、LLM 服务和统计接口。

前端使用 Vue 3、Vite、Element Plus、Axios、ECharts 和 lucide-vue-next。页面采用登录页与登录后 AppShell 分离的结构，登录后统一使用顶部导航、左侧 SideRail、主工作区和全局 AI 助手。

数据与外部服务包括 MySQL、Neo4j Desktop 本地实例，以及已接入的 DeepSeek 大模型服务。当前系统通过环境变量安全读取 API Key；在 LLM 未启用或调用失败时，也能通过本地规则和知识库结果生成兜底建议。

## 系统架构

```text
Vue 3 + Vite 前端
  |
  |  /api 代理
  v
Spring Boot 后端服务 :8088
  |
  |-- MySQL troubleshoot_kg
  |     |-- 用户、角色、故障知识库、分析历史
  |
  |-- Neo4j MistMoe / neo4j
  |     |-- 故障关系图谱
  |
  |-- LLM Provider
        |-- DeepSeek API
        |-- 智能排障建议生成
        |-- 全局 AI 助手聊天回复
```

## 本机环境约定

| 服务 | 配置 |
| --- | --- |
| MySQL 数据库 | `troubleshoot_kg` |
| MySQL 地址 | `localhost:3306` |
| MySQL 用户 | `root` |
| MySQL 密码 | `123456` |
| Neo4j 实例 | `MistMoe` |
| Neo4j 数据库 | `neo4j` |
| Neo4j Bolt | `bolt://127.0.0.1:7687` |
| Neo4j HTTP | `http://127.0.0.1:7474/db/neo4j/tx/commit` |
| Neo4j 用户 | `neo4j` |
| Neo4j 密码 | `root123456` |
| 后端端口 | `8088` |
| 前端端口 | `5173` |
| JDK | `D:\db\jdk17.0.19_10` |
| Maven | `D:\javaweb-troubleshoot-kg-llm\tools\apache-maven-3.9.16` |

这些配置用于本地课程演示。正式环境不应该把数据库密码和大模型密钥写死在仓库中，应改为环境变量或部署平台密钥配置。

## 启动后端

PowerShell：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
..\tools\apache-maven-3.9.16\bin\mvn.cmd spring-boot:run
```

后端启动成功后访问：

```text
http://localhost:8088/api/health
```

如果返回健康状态，说明 Spring Boot 服务已经启动。

如果希望启动后端时启用自己的 DeepSeek API Key，可以使用项目提供的本地脚本：

```powershell
cd D:\javaweb-troubleshoot-kg-llm
.\scripts\start-backend-deepseek.ps1
```

脚本会提示输入 DeepSeek API Key，输入内容不会写入仓库文件，只会作为当前后端进程的环境变量传给 Spring Boot。这个方式可以脱离老师账号，长期用于本地演示智能排障页和全局 AI 助手的大模型回复能力。

如果 PowerShell 提示脚本执行策略限制，可以改用：

```powershell
cd D:\javaweb-troubleshoot-kg-llm
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend-deepseek.ps1
```

## 启动前端

PowerShell：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run dev
```

前端默认访问地址：

```text
http://localhost:5173
```

Vite 已配置 `/api` 代理到后端 `http://localhost:8088`。如果只在本机演示，启动前端和后端即可。

如果需要映射给别人访问，可以让 Vite 监听局域网地址：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run dev -- --host 0.0.0.0
```

对外映射时要注意：前端端口 `5173` 只能展示页面，如果页面还要调用后端接口，也需要让外部能够访问后端 `8088`，或者重新配置公网环境下的 API 地址。

## 初始化数据库

MySQL 初始化：

```sql
source D:/javaweb-troubleshoot-kg-llm/database/mysql_schema.sql;
source D:/javaweb-troubleshoot-kg-llm/database/mysql_init.sql;
```

Neo4j 初始化方式有两种：

- 推荐方式：先启动 Neo4j Desktop Studio 的 `MistMoe` 实例，再到前端知识图谱页点击“从 MySQL 导入图谱”。
- 接口方式：调用 `POST /api/kg/import`，后端会读取 MySQL 故障案例并写入 Neo4j。

仓库中也保留了 `database/neo4j_init.cypher`，用于查看图谱初始化语句和后续手动调试。

## 演示账号

当前演示阶段所有账号密码均为：

```text
123456
```

| 用户名 | 角色 | 说明 |
| --- | --- | --- |
| `admin` | `ADMIN` | 系统管理员 |
| `maintainer` | `MAINTAINER` | 知识库维护员 |
| `user01` | `USER` | 普通用户 |

当前密码仍是课程演示用的明文密码。后续如果要作为更完整的系统，应使用 BCrypt 等方式进行密码哈希存储。

## 角色与权限逻辑

系统中用户和角色是多对多关系。用户登录后，后端返回该用户拥有的角色，前端根据角色控制菜单可见性和页面入口。

| 角色 | 主要定位 | 典型页面 |
| --- | --- | --- |
| `ADMIN` | 管理整个系统 | 首页、智能排障、分析历史、故障知识库、我的收藏、知识图谱、用户管理、角色管理、个人资料 |
| `MAINTAINER` | 维护知识与审核投稿 | 首页、智能排障、分析历史、故障知识库、我的收藏、知识图谱、个人资料 |
| `USER` | 排障、浏览知识和沉淀个人经验 | 首页、智能排障、个人分析历史、故障知识库、我的收藏、知识图谱、个人资料 |

用户管理页用于创建、编辑、启停账号并分配角色。角色管理页用于解释当前角色体系和权限矩阵，目前更偏说明和查看，暂不做动态权限编辑。

## 主要页面说明

### 登录与注册

登录页和注册页已经完成高级视觉改造，采用浅蓝灰背景、整卡背景图、直角分区、低对比浮雕和极细线几何星芒标识。登录页不显示全局 AI 助手，避免干扰认证流程。

### AppShell 全局布局

登录后页面统一进入 AppShell。AppShell 包含顶部栏、左侧 SideRail、主工作区和全局 AI 助手。侧边栏使用 lucide-vue-next 线性图标，保持低对比、清冷、轻浮雕风格。

### 首页工作台

首页已改为“清冷白色浮雕工作台”风格，不再是普通后台 Dashboard。首页包含 Workspace Overview、今日排障状态环、系统连接状态、LLM 模型服务状态、最近分析记录和故障类型分布。快捷入口被融入各组件中，全局 AI 助手改为右下角悬浮入口。

### 智能排障

智能排障页是系统的核心业务页。用户输入报错、技术栈和上下文后，前端调用 `POST /api/error/analyze`。后端会匹配知识库、检索图谱、调用 DeepSeek 生成建议，并把结果保存到分析历史。分析完成后可以直接导出 Word，或打开浏览器打印窗口生成 PDF。

### 故障知识库

故障知识库页用于浏览和维护 JavaWeb 常见故障案例。三个角色都能查询、查看详情和收藏；管理员与维护员可以新增、编辑、删除、审核经验投稿并同步到 Neo4j。用户可以粘贴报错日志和修复过程，调用 DeepSeek 整理成可编辑候选案例，再提交审核。

### 我的收藏

我的收藏页按当前登录账号读取 `error_case_favorite` 数据，支持关键词检索、案例详情、取消收藏和返回知识库。它让普通用户也能把常用排障经验沉淀为个人速查清单。

### 知识图谱

知识图谱页用于展示 Neo4j 中的故障关系。页面支持从 MySQL 导入图谱、按案例查看关系图、搜索节点，并展示节点详情。图谱节点类型包括故障、现象、环节、原因、方案、文件、命令和组件。

### 分析历史

分析历史页用于复盘每一次智能排障请求。页面会展示历史记录、命中状态、技术栈分布和复盘详情。用户可以查看完整输入、AI 建议、匹配案例和图谱快照，并将选中记录导出为 Word 或 PDF。

### 个人资料

个人资料页采用平面双栏账户工作区。三个角色都可以修改自己的显示名称、邮箱和密码，保存后顶部用户信息同步更新；用户名、角色和账号状态由管理员维护，个人不能自行修改。

### 用户管理

用户管理页调用真实后端接口，支持用户列表、关键词筛选、角色筛选、状态筛选、新增用户、编辑用户和启停账号。这个页面体现了系统的多角色管理能力。

### 角色管理

角色管理页用于展示系统内置角色和权限矩阵。当前角色不做动态编辑，主要用于说明不同账号为什么能看到不同页面，以及系统权限如何组织。

### 全局 AI 助手

AI 助手已从首页局部组件改为全局组件，挂载在 AppShell 中。登录后的所有业务页面都会显示 AI 助手入口，登录页和注册页不会显示。当前 AI 助手支持展开聊天窗口、快捷问题和输入问题，并通过 `/api/assistant/chat` 请求后端；当后端使用 DeepSeek 脚本启动时，会调用真实大模型生成回复，调用失败时回退到本地兜底建议。

## 后端接口清单

健康检查：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/health` | 后端健康检查 |

认证：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/auth/login` | 登录 |
| `GET` | `/api/auth/profile` | 获取当前用户资料 |
| `PUT` | `/api/auth/profile` | 当前用户修改自己的昵称和邮箱 |
| `PUT` | `/api/auth/password` | 校验原密码并修改当前用户密码 |

故障知识库：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/error/list` | 查询故障案例，支持 `keyword`、`component`、`stage` |
| `POST` | `/api/error/create` | 新增故障案例 |
| `PUT` | `/api/error/{id}` | 更新故障案例 |
| `DELETE` | `/api/error/{id}` | 删除故障案例 |

案例收藏：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/favorites/cases` | 查询当前用户收藏案例 |
| `GET` | `/api/favorites/case-ids` | 查询当前用户已收藏的案例 ID |
| `POST` | `/api/favorites/cases/{caseId}` | 收藏指定故障案例 |
| `DELETE` | `/api/favorites/cases/{caseId}` | 取消收藏指定故障案例 |

经验投稿与审核：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/submissions/draft` | 调用 DeepSeek 整理投稿候选字段 |
| `POST` | `/api/submissions/create` | 提交经验进入待审核状态 |
| `GET` | `/api/submissions/list` | 按状态或投稿用户查询投稿列表 |
| `GET` | `/api/submissions/{id}` | 查询投稿详情 |
| `PUT` | `/api/submissions/{id}/approve` | 管理员或维护员审核通过并写入知识库 |
| `PUT` | `/api/submissions/{id}/reject` | 管理员或维护员驳回投稿 |

智能排障：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/error/analyze` | 提交报错并生成排障分析 |

全局 AI 助手：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/assistant/chat` | 全局 AI 助手聊天接口，DeepSeek 启用时调用真实模型 |

知识图谱：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/kg/import` | 从 MySQL 导入 Neo4j 图谱 |
| `GET` | `/api/kg/graph/{errorId}` | 获取某个故障案例的关系图 |
| `GET` | `/api/kg/search` | 搜索图谱节点，支持 `keyword` |

分析历史：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/analysis-history/list` | 查询分析历史，支持 `userId`、`keyword`、`matched` |
| `GET` | `/api/analysis-history/{id}` | 查看分析历史详情 |
| `DELETE` | `/api/analysis-history/{id}` | 删除分析历史 |

用户与角色：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/users/list` | 查询用户列表，支持 `keyword`、`roleCode`、`status` |
| `GET` | `/api/users/roles` | 查询角色列表 |
| `POST` | `/api/users/create` | 新增用户 |
| `PUT` | `/api/users/{id}` | 更新用户 |
| `PUT` | `/api/users/{id}/status` | 启用或禁用用户 |

## MySQL 数据表

| 表名 | 作用 |
| --- | --- |
| `sys_user` | 用户账号信息 |
| `sys_role` | 系统角色信息 |
| `sys_user_role` | 用户与角色关联关系 |
| `error_case` | 故障案例知识库 |
| `analysis_history` | 智能排障分析历史 |
| `error_case_submission` | 用户修复经验投稿、AI 候选字段与审核状态 |
| `error_case_favorite` | 用户与收藏故障案例的关联关系 |

`error_case` 是知识库和图谱导入的核心数据源。`analysis_history` 保存用户每次排障请求的输入、命中案例、图谱快照和建议内容。`error_case_submission` 保存用户投稿、DeepSeek 整理结果和审核信息，审核通过后关联正式案例。`error_case_favorite` 支撑三个角色的个人收藏。`sys_user`、`sys_role`、`sys_user_role` 支撑登录和多角色页面权限。

## Neo4j 图谱模型

当前图谱围绕故障案例展开，主要节点类型包括：

| 节点类型 | 含义 |
| --- | --- |
| `Error` | 故障或报错类型 |
| `Symptom` | 报错现象 |
| `Stage` | 发生环节 |
| `Component` | 技术组件 |
| `Cause` | 可能原因 |
| `Solution` | 解决方案 |
| `File` | 相关配置或代码文件 |
| `Command` | 排查命令 |

主要关系包括：

| 关系 | 含义 |
| --- | --- |
| `OCCURS_IN` | 故障发生在某个环节 |
| `HAS_SYMPTOM` | 故障具有某种现象 |
| `MAY_CAUSED_BY` | 故障可能由某个原因导致 |
| `SOLVED_BY` | 故障可由某个方案解决 |
| `RELATED_TO` | 故障与某个组件相关 |
| `CHECK_FILE` | 排查时需要检查某个文件 |
| `USE_COMMAND` | 排查时可以使用某条命令 |

## LLM 配置说明

当前后端已经接入 DeepSeek 大模型，主要用于两类能力：

- 智能排障页：结合知识库匹配结果和 Neo4j 图谱证据生成结构化排障建议；
- 全局 AI 助手：通过 `/api/assistant/chat` 进行问答回复。

项目不把 DeepSeek API Key 写入配置文件，而是通过环境变量读取：

```yaml
llm:
  enabled: true
  provider: deepseek
  api-url: https://api.deepseek.com/chat/completions
  api-key: ${LLM_API_KEY:}
  model: deepseek-chat
```

本地演示推荐使用脚本启动后端：

```powershell
cd D:\javaweb-troubleshoot-kg-llm
.\scripts\start-backend-deepseek.ps1
```

脚本会隐藏输入 DeepSeek API Key，并只在当前后端进程中设置 `LLM_API_KEY`。如果没有提供 API Key，或者模型调用失败，系统不会中断业务流程，而是自动回退到知识库和本地规则生成兜底建议。

## 测试与构建

后端测试：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
..\tools\apache-maven-3.9.16\bin\mvn.cmd test
```

如果本机依赖已经下载完成，也可以使用离线仓库运行测试：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$repo='C:\Users\97780\.m2\repository'
..\tools\apache-maven-3.9.16\bin\mvn.cmd "-Dmaven.repo.local=$repo" -o test
```

前端构建：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

最近一次验证中，前端 UI 契约与生产构建均已通过，后端完整测试共 `31` 项，结果为 `0 failures / 0 errors`。正式展示前仍建议重新运行一次，以确认本机数据库与环境变量状态。

## 当前完成度

已完成：

- 项目骨架：Spring Boot 后端、Vue 3 前端、Vite 代理和基础目录结构。
- MySQL 数据库：用户、角色、故障案例、分析历史表结构和种子数据。
- Neo4j 图谱：从 MySQL 导入故障案例并生成关系图谱。
- 登录与注册：高端浅蓝灰视觉风格，独立于登录后 AppShell。
- 多角色登录：管理员、维护员、普通用户三类账号。
- 个人资料：三个角色都能修改昵称、邮箱和密码，资料保存后同步顶部用户信息。
- 首页工作台：清冷白色浮雕 SaaS 控制台风格。
- 智能排障：真实后端接口链路，包含知识库匹配、图谱证据和历史保存。
- 故障知识库：真实 CRUD、个人收藏、用户经验投稿、DeepSeek 字段整理、审核和同步图谱入口。
- 我的收藏：真实保存、查询和取消收藏，三个角色均可使用。
- 知识图谱：真实图谱查询、搜索和可视化展示。
- 分析历史：真实历史查询、详情复盘、删除和 Word / PDF 导出。
- 用户管理：真实用户列表、创建、编辑、启停和角色分配。
- 角色管理：角色说明和权限矩阵展示。
- DeepSeek 大模型接入：支持通过本地脚本输入 API Key 并启用真实模型调用。
- 全局 AI 助手：登录后页面全局显示，已接入后端 `/api/assistant/chat` 聊天接口。
- Apache ECharts：用于首页排障趋势、故障分布、知识库组件趋势和知识图谱关系展示。
- 阶段文档：每个主要阶段都有独立开发记录。

待继续完善：

- 首页统计数据进一步真实化，减少静态展示。
- 后端权限校验增强，目前主要是前端菜单和页面入口控制。
- 密码加密存储和更严格的登录态机制。
- 部署环境配置，包括公网 API 地址、跨域、安全密钥和日志。
- 更丰富的报告模板、真实首页统计接口和更完整的演示数据。

## 阶段记录文档

开发过程中的阶段说明保存在 `docs/phase-notes/`，用于回顾每一步做了什么、为什么这样做、后续如何衔接。当前主要记录包括：

- `phase-0-project-skeleton.md`
- `phase-0.5-multi-role-foundation.md`
- `phase-1-error-knowledge-base.md`
- `phase-2-neo4j-knowledge-graph.md`
- `phase-3-error-analysis-flow.md`
- `phase-4-llm-advice-generation.md`
- `phase-5-login-visual-polish.md`
- `phase-5-auth-card-overlay-revision.md`
- `phase-6-app-shell-dashboard.md`
- `phase-6-dashboard-neumorphic-revision.md`
- `phase-6-global-ai-assistant.md`
- `phase-7-page-capability-map.md`
- `phase-8-smart-troubleshoot-page.md`
- `phase-9-error-knowledge-base-page.md`
- `phase-10-knowledge-graph-page.md`
- `phase-11-analysis-history-page.md`
- `phase-12-user-management-page.md`
- `phase-13-role-management-page.md`
- `phase-14-ai-assistant-interaction-fix.md`
- `phase-15-deepseek-startup-script.md`
- `phase-16-global-ai-assistant-backend-chat.md`
- `phase-17-report-export-and-echarts.md`
- `phase-18-knowledge-submission-review.md`
- `phase-19-knowledge-readonly-favorites.md`
- `phase-20-profile-and-favorites-pages.md`
- `phase-21-stat-card-style-isolation.md`
- `phase-22-auth-copy-localization-and-spacing.md`
- `phase-23-neumorphic-foundation-dashboard.md`
- `phase-24-neumorphic-analyze-page.md`
- `phase-25-neumorphic-history-page.md`
- `phase-26-neumorphic-knowledge-page.md`
- `phase-27-data-heading-knowledge-favorites.md`
- `phase-28-neumorphic-knowledge-graph.md`
- `phase-29-neumorphic-management-pages.md`
- `phase-30-flat-profile-workspace.md`

## 常见问题

如果前端能打开但登录或接口失败，通常是后端没有启动，或者公网映射时只映射了前端 `5173`，没有让后端 `8088` 对外可访问。

如果知识图谱为空，先确认 Neo4j Desktop 的 `MistMoe` 实例已经启动，然后在知识图谱页点击“从 MySQL 导入图谱”。

如果分析历史为空，先到智能排障页提交一次报错分析。系统完成分析后会自动写入历史记录。

如果全局 AI 助手或智能排障建议仍然像本地兜底回复，先确认后端是否通过 `scripts/start-backend-deepseek.ps1` 启动，并且启动时输入了有效的 DeepSeek API Key。前端页面需要刷新后再测试。

如果 Maven 首次运行很慢或失败，通常是依赖还没有下载完成，需要允许 Maven 联网下载依赖。依赖下载完成后，后续本地测试会快很多。

如果中文在终端中显示乱码，优先检查 PowerShell 编码或编辑器编码。项目文档本身应以 UTF-8 保存。
