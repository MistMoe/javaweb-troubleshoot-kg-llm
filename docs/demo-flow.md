# JavaWeb Troubleshoot KG LLM 完整演示流程

这是一份可以在课程答辩、项目验收或录屏时直接照着执行的演示手册。它不按 PPT 页数组织，而是按真实用户完成一次排障、复盘和知识沉淀的业务路径组织。

## 一、演示主线

整场演示围绕下面这句话展开：

```text
用户提交真实 JavaWeb 报错，系统匹配 MySQL 知识库、检索 Neo4j 图谱、调用 DeepSeek 生成建议，
再把结果保存为历史记录、导出报告；用户还可以收藏案例或投稿修复经验，经审核后继续补充知识库和图谱。
```

推荐演示顺序：

1. 管理员登录，快速展示首页系统状态。
2. 在智能排障页提交一段完整 MySQL 认证异常。
3. 查看知识库命中、图谱证据、DeepSeek 建议并导出报告。
4. 到分析历史复盘刚才的记录，再演示 Word / PDF 导出。
5. 到故障知识库搜索并收藏案例，在“我的收藏”中查看。
6. 切换普通用户，投稿一条 Jackson 序列化修复经验。
7. 切换管理员或维护员，审核投稿并写入正式知识库。
8. 同步 Neo4j，在知识图谱中查看新案例关系。
9. 使用全局 AI 助手追问一个排障问题。
10. 展示个人资料真实修改和三角色权限差异。

完整路线约 12 到 18 分钟；文末提供 5 分钟精简路线。

## 二、演示前准备

### 2.1 服务检查

| 服务 | 演示前状态 | 检查方法 |
| --- | --- | --- |
| MySQL | `troubleshoot_kg` 数据库可连接 | Navicat 查看 `error_case`、`analysis_history` 等表 |
| Neo4j | `MistMoe` 实例已启动 | Neo4j Desktop 显示绿色运行状态 |
| DeepSeek | 准备有效 API Key | 使用项目脚本启动后端时输入 |
| 后端 | 监听 `8088` | 打开 `http://localhost:8088/api/health` |
| 前端 | 监听 `5173` | 打开 `http://localhost:5173` |

如果数据库尚未初始化：

```sql
source D:/javaweb-troubleshoot-kg-llm/database/mysql_schema.sql;
source D:/javaweb-troubleshoot-kg-llm/database/mysql_init.sql;
```

初始化脚本已经包含知识投稿表 `error_case_submission` 和收藏表 `error_case_favorite`。如果使用较早的数据库，可单独执行：

```sql
source D:/javaweb-troubleshoot-kg-llm/docs/sql/2026-07-10-error-case-submission.sql;
source D:/javaweb-troubleshoot-kg-llm/docs/sql/2026-07-10-error-case-favorite.sql;
```

### 2.2 推荐启动方式

先启动 MySQL 和 Neo4j，再在项目根目录启动带 DeepSeek 的后端：

```powershell
cd D:\javaweb-troubleshoot-kg-llm
.\scripts\start-backend-deepseek.ps1
```

脚本提示时粘贴自己的 DeepSeek API Key。输入不会写入仓库，只在本次后端进程中使用。

如果本次不演示真实大模型，可以使用普通启动命令：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
..\tools\apache-maven-3.9.16\bin\mvn.cmd spring-boot:run
```

另开一个 PowerShell 启动前端：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run dev
```
访问：
```text
http://localhost:5173
```


```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
npm.cmd run preview -- --host 0.0.0.0 --port 4173
```
访问：
```text
http://mistmoe.w1.luyouxia.net
```


### 2.3 演示账号

| 用户名 | 密码 | 角色 | 演示用途 |
| --- | --- | --- | --- |
| `admin` | `123456` | 系统管理员 | 完整主流程、用户管理、角色管理、投稿审核 |
| `maintainer` | `123456` | 知识库维护员 | 知识维护和投稿审核 |
| `user01` | `123456` | 普通用户 | 排障、收藏、投稿和个人资料 |

建议先用 `admin` 走主流程，需要展示权限差异和投稿时再切换 `user01`。

### 2.4 演示数据预热

为避免现场等待或空数据，提前完成：

1. 在知识图谱页执行一次“从 MySQL 导入图谱”。
2. 确认知识库至少包含“端口被占用”“MySQL 账号密码错误”“大模型 API Key 无效”。
3. 确认分析历史中至少已有一条记录。
4. 如果准备现场演示投稿审核，先不要提前提交本文提供的 Jackson 案例，避免重复。

## 三、管理员登录与首页

### 3.1 登录

操作：

1. 打开 `http://localhost:5173`。
2. 用户名输入 `admin`。
3. 密码输入 `123456`。
4. 点击“登录系统”。

讲解建议：

```text
登录和业务工作台相互独立。登录成功后，后端返回当前用户及角色，前端据此生成可见菜单；
AI 助手只在登录后的 AppShell 中出现。
```

### 3.2 首页工作台

进入首页后先停留 10 秒，不要立刻切页。按从上到下顺序指出：

- 系统运行、Neo4j 和 LLM 服务状态。
- Apache ECharts 展示的一周排障趋势和成功率变化。
- MySQL、Neo4j、后端、前端连接状态。
- 当前 DeepSeek 模型服务、调用次数和平均延迟。
- 最近分析记录与故障类型分布。

过渡话术：

```text
首页只负责回答“系统是否可用、最近发生了什么”。下面进入真正的业务入口，提交一次完整报错。
```

## 四、核心演示：智能排障

### 4.1 主案例：Spring Boot 连接 MySQL 认证失败

进入“智能排障”，技术栈选择 `MySQL`。在“报错日志 / 异常信息”中完整粘贴：

```text
项目环境：Spring Boot 3.3.5、MySQL 8.0、JDK 17。
启动应用后 HikariCP 初始化失败，访问任何需要数据库的接口都会返回 500。

org.springframework.jdbc.CannotGetJdbcConnectionException:
Failed to obtain JDBC Connection
    at org.springframework.jdbc.datasource.DataSourceUtils.getConnection(DataSourceUtils.java:84)
    at org.springframework.jdbc.core.JdbcTemplate.execute(JdbcTemplate.java:653)
Caused by: java.sql.SQLException:
Access denied for user 'root'@'localhost' (using password: YES)
    at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:130)
    at com.mysql.cj.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:825)
    at com.zaxxer.hikari.pool.PoolBase.newConnection(PoolBase.java:360)

application.yml 中配置：
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/troubleshoot_kg
spring.datasource.username=root

已尝试操作：确认 MySQL 服务已启动，3306 端口可以连接，但应用仍然认证失败。
```

点击“开始智能分析”。分析期间说明系统依次执行：

1. 提取日志关键词与技术栈。
2. 匹配 MySQL `error_case` 知识库。
3. 查询 Neo4j 关联节点和关系。
4. 将原始报错、命中案例和图谱证据交给 DeepSeek 组织排查建议。
5. 把完整结果写入 `analysis_history`。

结果出现后依次展示：

- 分析流程四个步骤的状态。
- 命中的“MySQL 账号密码错误”案例和匹配置信度。
- 可能原因与解决方案。
- 相关文件 `application.yml`。
- 建议命令或 SQL，例如 `SELECT user,host FROM mysql.user;`。
- Neo4j 图谱证据中的 Error、Cause、Solution、File、Command、Component 节点。
- AI 建议是否带有针对当前日志的排查顺序，而不是只重复知识库原文。

讲解重点：

```text
这不是把报错直接发给大模型。系统先用本地知识库和图谱提供可追溯证据，再让 DeepSeek 生成可执行建议；
即使模型接口暂时不可用，知识库匹配和本地兜底仍然可以返回结果。
```

### 4.2 现场导出

分析结果生成后：

1. 点击 `Word`，确认浏览器下载排障报告。
2. 点击 `PDF`，系统打开打印窗口；选择“另存为 PDF”即可生成文件。

说明报告包含原始输入、命中案例、原因、方案、AI 建议和图谱摘要，适合提交工单或课程材料。

### 4.3 备用输入一：端口占用

如果主案例被提前演示过，可换用：

```text
APPLICATION FAILED TO START

Description:
Web server failed to start. Port 8080 was already in use.

Action:
Identify and stop the process that's listening on port 8080
or configure this application to listen on another port.

环境：Spring Boot 3.3.5 / Windows 11 / JDK 17。
已尝试重启 IDEA，但端口仍然被占用，希望确认占用进程并安全处理。
```

技术栈选择 `Spring Boot`。预期命中“端口被占用”，并给出 `netstat -ano | findstr 8080` 和 `application.yml` 相关提示。

### 4.4 备用输入二：大模型认证失败

```text
调用 DeepSeek Chat Completions 接口失败。

HTTP/1.1 401 Unauthorized
{
  "error": {
    "message": "Authentication Fails, Your api key is invalid",
    "type": "authentication_error"
  }
}

Spring Boot 已通过环境变量读取 LLM_API_KEY，base URL 配置为
https://api.deepseek.com/chat/completions。
本地 curl 和后端请求都返回 401，希望判断是密钥、地址还是请求头问题。
```

技术栈选择 `LLM API`。预期命中“大模型 API Key 无效”。演示时不要展示真实 API Key。

## 五、分析历史与报告复盘

进入“分析历史”。

操作：

1. 在历史列表顶部找到刚生成的记录。
2. 点击记录，确认右侧标题与刚才的 MySQL 报错一致。
3. 展示原始报错输入、命中案例、知识图谱快照和 AI 建议。
4. 指出技术栈分布由真实历史记录统计。
5. 再次点击 `Word` 或 `PDF`，说明历史记录随时可以重新导出。

讲解建议：

```text
智能排障解决当下问题，分析历史解决后续复盘。一次分析不是阅后即焚，而是形成可查询、可导出的排障档案。
```

如果列表为空，说明后端没有保存成功；返回智能排障重新提交，并检查 `/api/analysis-history/list`。

## 六、故障知识库、收藏与个人速查

进入“故障知识库”。

### 6.1 搜索与详情

1. 搜索 `Access denied` 或 `MySQL`。
2. 点击“MySQL 账号密码错误”。
3. 在详情区展示报错现象、可能原因、解决方案、相关文件、命令和风险提示。
4. 指出组件分布图由 Apache ECharts 根据知识库数据生成。

### 6.2 收藏案例

1. 点击案例行或详情区的“收藏”。
2. 从左侧进入“我的收藏”。
3. 搜索 `MySQL`，选中刚收藏的案例。
4. 展示收藏详情和收藏时间。
5. 可以演示“取消收藏”，也可以保留作为普通用户的个人速查案例。

讲解建议：

```text
正式知识库属于系统，收藏属于当前账号。普通用户虽然不能编辑正式案例，但可以浏览和建立自己的排障速查清单。
```

## 七、完整投稿示例：用户经验沉淀与审核

这一段最能体现系统从“使用知识”扩展到“补充知识”。建议使用 `user01` 投稿，再使用 `admin` 或 `maintainer` 审核。

### 7.1 普通用户投稿

退出管理员账号，登录：

```text
用户名：user01
密码：123456
```

进入“故障知识库”，点击“投稿经验”。按下面内容填写。

#### 报错日志 / 异常堆栈

```text
项目环境：Spring Boot 3.3.5、JDK 17，接口返回实体中包含 LocalDateTime 字段。
访问 GET /api/orders/12 时接口返回 500。

org.springframework.http.converter.HttpMessageNotWritableException:
Could not write JSON: Java 8 date/time type `java.time.LocalDateTime`
not supported by default: add Module
"com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
    at org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter.writeInternal(
        AbstractJackson2HttpMessageConverter.java:492)
Caused by: com.fasterxml.jackson.databind.exc.InvalidDefinitionException:
Java 8 date/time type `java.time.LocalDateTime` not supported by default
    through reference chain: OrderResponse["createTime"]

项目中手动声明了一个 new ObjectMapper() 的 Bean，之后才出现该问题。
```

#### 技术栈与发生环节

```text
技术栈：Spring Boot
发生环节：接口响应序列化
```

#### 我的解决过程

```text
先检查 pom.xml，项目中已经间接包含 jackson-datatype-jsr310，因此不是简单缺少依赖。
继续检查自定义 JacksonConfig，发现手动创建 ObjectMapper 后没有注册 JavaTimeModule，
也绕过了 Spring Boot 的自动配置。

最终删除不必要的自定义 ObjectMapper Bean，改为注入 Spring Boot 管理的 ObjectMapper；
如果必须自定义，则执行 objectMapper.registerModule(new JavaTimeModule())，
并按接口要求配置 LocalDateTime 的序列化格式。

重启后再次请求 GET /api/orders/12，createTime 正常返回，接口状态码恢复为 200。
```

#### 相关文件

```text
pom.xml, JacksonConfig.java, OrderResponse.java
```

#### 相关命令

```text
mvn dependency:tree | findstr jackson
```

点击“AI 整理候选案例”。等待 DeepSeek 返回后，重点检查右侧候选字段是否包含：

- 故障名称：类似“LocalDateTime 序列化失败”。
- 关键词：`LocalDateTime`、`JavaTimeModule`、`InvalidDefinitionException`、`ObjectMapper`。
- 报错现象：接口写 JSON 时返回 500。
- 可能原因：自定义 ObjectMapper 未注册 JavaTimeModule，绕过 Spring Boot 自动配置。
- 解决方案：删除不必要的 Bean，或注册 JavaTimeModule 后验证接口。
- 相关文件与命令：应保留上面填写的内容。
- 风险提示：不要为了修复单个字段随意改变全局时间格式；升级 Jackson 依赖时保持版本一致。

AI 结果不是直接入库。可以现场手动修改一两个字段，说明候选内容仍由人确认。确认后点击“提交审核”。

### 7.2 管理员或维护员审核

退出 `user01`，登录 `admin / 123456` 或 `maintainer / 123456`。

1. 进入故障知识库，点击“待审核”。
2. 选择刚才的 Jackson 投稿。
3. 左侧核对用户原始日志和修复过程，右侧核对结构化候选字段。
4. 如有需要，修改故障名称、关键词或风险提示。
5. 点击“审核通过”。
6. 返回故障案例列表，搜索 `LocalDateTime`，确认正式案例已经出现。

讲解建议：

```text
普通用户提供真实修复经验，DeepSeek 负责把非结构化文字整理成候选字段，
管理员或维护员负责最终质量控制。只有审核通过的数据才进入正式 error_case 表。
```

如果不希望污染演示数据库，可以点击“驳回”并填写“演示数据，不入正式知识库”；但这样无法继续演示新案例同步图谱。

## 八、知识图谱同步与关系展示

进入“知识图谱”。

1. 点击“从 MySQL 导入图谱”。
2. 选择刚审核通过的 `LocalDateTime` 案例；如果没有审核该案例，可选择“大模型 API Key 无效”。
3. 点击“查看关系”。
4. 拖动画布并滚轮缩放，保持节点舒展。
5. 点击 Error、Cause、Solution、File 或 Command 节点，查看右侧节点详情。
6. 搜索 `JavaTimeModule`、`application.yml`、`MySQL` 等关键词验证图谱检索。

讲解建议：

```text
MySQL 保存完整案例，Neo4j 保存可遍历关系。知识图谱不是装饰图，而是智能排障返回“图谱证据”的数据来源。
```

## 九、全局 AI 助手

在任意登录后页面点击右下角“AI 助手”。

推荐直接输入下面的问题，而不是只点击快捷按钮：

```text
Spring Boot 3 项目手动创建 ObjectMapper 后，LocalDateTime 返回 JSON 失败。
请按“先检查什么、如何修复、如何验证、有哪些风险”的顺序给出建议。
```

第二个追问：

```text
如果 jackson-datatype-jsr310 已经在依赖树中，为什么仍然会报不支持 LocalDateTime？
```

观察回复是否结合当前上下文解释自定义 ObjectMapper、JavaTimeModule 和 Spring Boot 自动配置。切换到其他页面时 AI 助手入口仍然存在，说明它挂载在全局 AppShell，而不是某一个页面内部。

辨别真实 DeepSeek 回复的方法：回复通常会针对输入细节展开，生成速度受网络影响；如果总是立即返回固定短文，并提示“本地兜底建议”，应检查后端启动方式和 API Key。

## 十、个人资料真实编辑

点击顶部用户区进入“个人资料”。

1. 指出左侧身份栏包含当前角色、账号状态、分析/收藏/投稿统计。
2. 在右侧把显示名称临时改为“演示管理员”或“演示用户”。
3. 点击“保存资料”，观察顶部用户名称同步变化。
4. 展示用户名、角色、状态为只读，不能自行越权修改。
5. 密码区可以说明真实支持原密码校验；现场不建议修改管理员密码，以免影响后续演示。

如果要验证密码修改，建议使用 `user01`，修改后退出并用新密码重新登录；演示结束后再改回 `123456`。

## 十一、用户管理与角色边界

使用管理员账号进入“用户管理”：

1. 展示账号列表和角色筛选。
2. 打开新增用户或编辑用户面板，说明可设置昵称、邮箱、启停状态和角色。
3. 不要现场停用 `admin`。

进入“角色设置”：

- `ADMIN` 管理用户、角色和全部数据。
- `MAINTAINER` 维护知识库、审核投稿和同步图谱。
- `USER` 使用排障、个人历史、知识库浏览、收藏、投稿和个人资料。

角色设置页当前主要展示固定三角色和权限矩阵，不支持现场随意删除角色，避免破坏课程演示数据。

## 十二、演示收尾话术

```text
这个系统已经形成了两条闭环：

第一条是排障闭环：输入报错 -> 匹配知识库 -> 检索 Neo4j -> DeepSeek 建议 -> 历史复盘 -> 报告导出。

第二条是知识成长闭环：浏览案例 -> 收藏速查 -> 用户投稿 -> AI 结构化 -> 人工审核 -> 正式知识库 -> 同步知识图谱。

MySQL 负责可靠保存结构化业务数据，Neo4j 负责表达故障关系，DeepSeek 负责在证据基础上组织建议，
三类角色则分别承担系统管理、知识维护和日常使用。
```

## 十三、常见现场问题与处理

### 后端启动提示 8088 被占用

说明已经有一个后端进程在运行。先检查：

```powershell
Get-NetTCPConnection -LocalPort 8088 -State Listen | Select-Object LocalAddress,LocalPort,OwningProcess
Get-Process -Id <OwningProcess>
```

确认是旧的本项目 Java 进程后再停止，不要直接结束不明进程。

### 登录失败

依次检查：

1. 后端 `8088` 是否启动。
2. MySQL 是否启动。
3. `sys_user` 是否包含演示账号。
4. 前端 `/api` 是否代理到 `http://localhost:8088`。

### 智能排障没有命中

- 使用本文提供的完整输入，保留 `Access denied for user` 等关键字。
- 确认 `error_case` 有种子数据。
- 技术栈选择与案例相近的 `MySQL` 或 `Spring Boot`。

### 图谱为空或导入失败

- 确认 Neo4j `MistMoe` 实例已启动。
- 当前数据库为 `neo4j`，Bolt 地址为 `bolt://127.0.0.1:7687`。
- 确认后端 Neo4j 用户名和密码正确。
- 先在知识图谱页点击“从 MySQL 导入图谱”。

### 投稿 AI 整理失败

- 确认后端由 `scripts/start-backend-deepseek.ps1` 启动。
- 确认 API Key 有效且网络能访问 DeepSeek。
- 即使 AI 整理失败，也可以手动填写右侧候选字段后提交。

### AI 助手回复过快或像固定文本

- 查看回复是否包含“本地兜底建议”。
- 确认后端进程中已设置 `LLM_API_KEY`，而不是只在另一个终端设置。
- 重启后端后刷新前端页面再测试。

### Word 可以下载但 PDF 没有文件

PDF 功能调用浏览器打印窗口，不会直接静默下载。需要在打印窗口选择“另存为 PDF”；如果窗口未出现，检查浏览器弹窗拦截。

### 公网映射只能打开页面，接口全部失败

只映射 `5173` 只能看到前端静态页面。需要同时让后端 `8088` 可访问，或调整 Vite / 部署环境的 API 代理。不要把 MySQL、Neo4j Bolt 或 DeepSeek API Key 直接暴露到公网。

## 十四、5 分钟精简路线

时间不足时只走以下步骤：

1. `admin / 123456` 登录，首页停留 15 秒说明系统状态和 ECharts 图表。
2. 智能排障粘贴本文 MySQL 认证失败主案例，展示知识库命中、图谱证据和 DeepSeek 建议。
3. 点击 Word 导出，再进入分析历史找到刚才记录。
4. 故障知识库搜索“MySQL”，收藏案例并进入“我的收藏”。
5. 打开投稿经验窗口，展示本文 Jackson 示例和“AI 整理候选案例”，不必现场完成审核。
6. 知识图谱选择已有案例查看关系。
7. 打开全局 AI 助手，输入一个具体追问。

这条路线仍能覆盖“真实分析、历史沉淀、报告导出、知识收藏、经验投稿、图谱证据和大模型交互”七项核心能力。
