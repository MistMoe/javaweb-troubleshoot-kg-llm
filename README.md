# JavaWeb 智能排障知识图谱系统

面向 JavaWeb 常见报错的知识管理与检索增强排障系统。以 **MySQL 管理故障案例、Neo4j 组织关联证据、DeepSeek 生成排查建议**，把知识维护、经验审核、智能分析和历史复盘串成完整流程。

后端基于 Java 17、Spring Boot 3 与 LangChain4j，前端使用 Vue 3、Element Plus 和 ECharts。当前以本地运行和个人作品集展示为主。

## 核心能力

| 模块 | 已实现能力 |
| --- | --- |
| 故障知识库 | 案例维护、分类检索、个人收藏、用户修复经验投稿及审核 |
| 知识图谱 | 故障、环节、组件、现象、原因、方案、文件、命令共 8 类实体；Cypher 构建关系并可视化 |
| 差分同步 | 对比 MySQL 案例内容指纹，只重建新增或变更案例；处理删除、旧关系替换和孤立辅助节点清理 |
| 混合检索 | 关键词评分与可选语义向量召回，去重排序后保留最多 5 个候选 |
| 证据增强 | 查询首选案例 1～3 跳子图；模型按需调用 LangChain4j 的案例、图谱证据工具 |
| 流式排障 | SSE 展示检索进度、工具调用和模型增量文本，支持停止分析 |
| 异常兜底 | 图谱失败保留案例证据；模型超时、调用失败或熔断时返回本地知识库建议 |
| 历史与报告 | 保存完整分析结果，支持历史复盘、Word 导出及浏览器打印 PDF |
| 工作台 | 多角色页面入口、用户管理、个人资料、全局 AI 助手与统计图表 |

## 排障流程与架构

```text
维护案例 / 审核投稿 → MySQL → 内容指纹差分同步 → Neo4j
                         │                         │
用户提交日志 → 关键词 + 可选向量召回 → 首选案例的 1～3 跳子图
                                      │
                         LangChain4j 只读证据工具 + 模型
                                      │
                         SSE 增量建议 → 分析历史 → 报告
```

主页面调用 `POST /api/error/analyze/stream`。案例与图谱证据先形成一次请求内的快照，再通过 `searchCases`、`queryGraph` 提供给模型；工具不执行任意 Cypher 或系统命令。

保留的 `POST /api/error/analyze` 同步接口复用检索与图谱降级逻辑，沿用原有模型调用方式。流式输出、工具调用与熔断由主页面的流式链路提供。

| 层次 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite 5、Element Plus、Axios / Fetch、ECharts、lucide-vue-next |
| 后端 | Java 17、Spring Boot 3.3.5、Spring JDBC、LangChain4j 1.10.0、Maven |
| 存储与模型 | MySQL、Neo4j 5、DeepSeek；独立配置的 Embedding 服务 |
| 验证 | JUnit、MockMvc、H2、隔离 Neo4j 引擎、本地模型 HTTP 模拟服务 |

## 快速启动

以下命令以仓库根目录为工作目录，Windows 使用 PowerShell。需要 Java 17、Maven、MySQL 8.x、Neo4j 5，以及满足 Vite 5 要求的 Node.js（18.x 或 20 及以上）。

### 1. 获取代码并初始化 MySQL

```powershell
git clone https://github.com/MistMoe/javaweb-troubleshoot-kg-llm.git
cd javaweb-troubleshoot-kg-llm
```

从仓库根目录进入 MySQL 客户端，执行：

```sql
CREATE DATABASE IF NOT EXISTS troubleshoot_kg DEFAULT CHARACTER SET utf8mb4;
USE troubleshoot_kg;
SOURCE database/mysql_schema.sql;
SOURCE database/mysql_init.sql;
```

新安装的完整建表脚本已包含经验投稿和收藏表。旧数据库缺少对应表时，可参考 [投稿补表脚本](docs/sql/2026-07-10-error-case-submission.sql) 和 [收藏补表脚本](docs/sql/2026-07-10-error-case-favorite.sql)。已有数据库不要反复导入种子案例；本次 GraphRAG 改造本身无需新增 MySQL 表。

### 2. 配置数据库、图谱与模型

首次克隆需要创建本地配置；已有配置时直接检查，不要覆盖：

```powershell
Copy-Item backend/src/main/resources/application.example.yml backend/src/main/resources/application.yml
```

[配置样例](backend/src/main/resources/application.example.yml) 中需要检查：

- `spring.datasource`：MySQL 连接、用户名和本机密码。
- `neo4j.http-url`：实际数据库的事务提交地址，例如 `http://127.0.0.1:7474/db/neo4j/tx/commit`；以及 `neo4j.username`、`neo4j.password`。
- `llm.enabled`：是否启用模型；`llm.api-key` 从 `LLM_API_KEY` 读取，`llm.api-url` 与 `llm.model` 默认指向 DeepSeek。

`application.yml` 已被 Git 忽略。向量服务与聊天模型独立配置：

| 环境变量 | 用途 |
| --- | --- |
| `LLM_ENABLED` | 设为 `true` 启用模型调用 |
| `LLM_API_KEY` | 聊天模型密钥 |
| `EMBEDDING_BASE_URL` | Embedding 服务的 API 根地址，保留服务要求的路径前缀 |
| `EMBEDDING_MODEL` | 对应服务支持的向量模型名称 |
| `EMBEDDING_API_KEY` | 托管向量服务需要的密钥，本地服务可留空 |

未配置向量地址时使用关键词检索；服务失败时也回退关键词，并在结果中说明。DeepSeek 聊天接口不能直接代替 Embedding 接口。完整配置与检索阈值说明见 [GraphRAG 实现与验收](docs/graphrag-implementation.md)。

### 3. 启动后端

确认 Java 17 和 Maven 已加入环境，在仓库根目录执行：

```powershell
mvn.cmd -f backend/pom.xml spring-boot:run
```

未配置或未启用模型时，仍可使用本地知识库建议。后端默认地址为 `http://localhost:8088`，可访问 [健康接口](http://localhost:8088/api/health) 检查进程；它不代表数据库、图谱和模型都已连通。

仓库还提供 [DeepSeek 本地启动脚本](scripts/start-backend-deepseek.ps1)，支持隐藏输入密钥。该脚本沿用开发机的 `D:\db\jdk17.0.19_10` 和仓库 `tools/apache-maven-3.9.16` 路径，**其他机器应使用上面的通用 Maven 命令，或先调整脚本路径**。`tools` 目录没有随仓库上传。

### 4. 启动前端并同步图谱

新开终端，从仓库根目录执行：

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

前端默认地址为 [http://localhost:5173](http://localhost:5173)，Vite 将 `/api` 代理到后端 `8088`。两套项目同时启动时前端端口可能冲突，以终端输出为准。

启动 Neo4j 并确认配置指向本系统使用的数据库。登录后在知识图谱页点击“从 MySQL 导入图谱”，或调用 `POST /api/kg/import`。已有图谱会在同步时迁移到带 `caseId` 的关系。

图谱库应专用于本系统的 8 类实体：同步会清理已删除案例、旧关系和孤立辅助节点，不应与其他应用共用同名标签。`database/neo4j_init.cypher` 保留作结构参考，正常演示使用 MySQL 同步入口。

## 演示账号与页面

导入种子数据后，下列账号的演示密码均为 `123456`：

| 用户名 | 角色 | 主要用途 |
| --- | --- | --- |
| `admin` | ADMIN | 用户管理、案例维护与审核 |
| `maintainer` | MAINTAINER | 知识维护、投稿审核与图谱同步 |
| `user01` | USER | 排障分析、浏览收藏、经验投稿与个人资料 |

页面包括首页、智能排障、故障知识库、知识图谱、分析历史、我的收藏、个人资料、用户与角色管理。角色管理页用于查看角色说明和权限矩阵，不提供动态权限编辑。

建议演示顺序：选择端口占用样例 → 查看检索进度和建议 → 打开图谱证据 → 复盘历史并导出报告 → 投稿经验并审核 → 再次同步图谱。验证异常分支时分别停用向量、图谱或模型服务，检查页面给出的降级说明。

## 关键设计与边界

- **增量同步**：扫描 MySQL 完整快照，对比 Neo4j 内容指纹后差分写入；不是 CDC 或实时消息同步。关系记录案例归属，避免共享原因 / 方案节点导致案例串线。
- **检索范围**：最多返回 5 个候选，首选案例查询 1～3 跳子图、最多 200 条路径。混合分数不是诊断置信度，当前没有提供检索提升百分比。
- **资源控制**：流式任务使用 4 个线程、长度 8 的等待队列；最多执行 6 次工具调用，模型等待不超过 45 秒及请求剩余预算。
- **失败处理**：图谱失败仍保留 MySQL 案例；模型连续失败 3 次后熔断 30 秒。失败后的不完整回答会被清除，再返回本地建议。
- **停止分析**：浏览器中断请求后阻止后续工具与结果发送；已发出的外部请求不能保证立即停止计费，已开始的历史提交也可能完成。

SSE 协议、打分规则、并发边界与完整验收步骤见 [详细说明](docs/graphrag-implementation.md)。

## 测试与构建

从仓库根目录执行后端测试：

```powershell
mvn.cmd -f backend/pom.xml test
```

前端检查与构建：

```powershell
cd frontend
npm.cmd run test:ui
npm.cmd run test:sse
npm.cmd run build
```

**2026-11-02 验证结果**：45 项后端测试全部通过，前端 UI 契约、SSE 中文分包测试和生产构建通过。

图谱测试使用隔离的真实 Neo4j 引擎，覆盖共享节点、重复同步、更新、删除、旧图迁移和事务回滚；模型协议使用本地 HTTP 模拟服务。测试不会连接实际业务 MySQL 或 Neo4j。

## 当前限制

真实 DeepSeek、向量服务及实际检索效果尚未联调；向量保存在单进程内存中，重启后重建。需要用实际模型和标注查询评估效果，不能仅凭功能接入断言向量检索更好。

当前登录令牌使用内存会话，演示账号密码仍为明文存储；部分权限依赖前端入口控制，服务端鉴权与数据隔离仍需加强。当前版本适合本地演示，不应直接作为安全完备的公网服务部署。

后续可完善检索评测、持久化向量索引、服务端权限和部署配置。详细阶段记录保留在 `docs/phase-notes`，历史描述与数量以当前代码及验证记录为准。

## 项目结构

```text
javaweb-troubleshoot-kg-llm/
├─ backend/                  # Spring Boot、检索、证据工具、SSE 与历史
├─ frontend/                 # Vue 工作台、ECharts 图谱、流式排障界面
├─ database/                 # MySQL schema / 种子数据、Cypher 结构参考
├─ scripts/                  # 开发机 DeepSeek 启动脚本
└─ docs/                     # 实现与验收、旧库补表、阶段开发记录
```
