# Phase 2 Neo4j Knowledge Graph

## 本阶段目标

把 Phase 1 中 MySQL 的 `error_case` 故障案例导入 Neo4j，形成可查询、可展示的排障知识图谱。

## 已完成内容

- 新增 Neo4j HTTP Transaction API 配置。
- 新增后端图谱接口：
  - `POST /api/kg/import`
  - `GET /api/kg/graph/{errorId}`
  - `GET /api/kg/search?keyword=xxx`
- 新增后端图谱响应结构：
  - `GraphNode`
  - `GraphLink`
  - `GraphResponse`
  - `KgImportResponse`
- 新增 `KgService`，从 MySQL `error_case` 读取故障案例并写入 Neo4j。
- 更新 `database/neo4j_init.cypher`，包含约束和最小手工种子示例。
- 前端新增知识图谱页：
  - 从 MySQL 导入图谱
  - 选择故障案例查看关系图
  - 搜索图谱节点
  - ECharts Graph 可视化节点和关系

## 技术选择说明

原计划可以使用 Neo4j Java Driver，但当前环境的 Maven 联网审批不可用，新增外部依赖会阻塞验证。

本阶段改用 Neo4j 自带 HTTP Transaction API：

```text
http://127.0.0.1:7474/db/neo4j/tx/commit
```

这样可以复用已有 Spring Web 和 Jackson，不新增 Maven 依赖，同时能完成真实 Neo4j 导入和查询。

## 图谱映射

MySQL `error_case` 到 Neo4j 的映射：

- `error_name` -> `Error.name`
- `id` -> `Error.mysqlId`
- `keyword` -> `Error.keyword`
- `stage` -> `Stage.name`
- `component` -> `Component.name`
- `symptom` -> `Symptom.name`
- `cause` -> `Cause.name`
- `solution` -> `Solution.name`
- `related_file` -> `File.name`
- `related_command` -> `Command.name`

关系：

- `(Error)-[:OCCURS_IN]->(Stage)`
- `(Error)-[:RELATED_TO]->(Component)`
- `(Error)-[:HAS_SYMPTOM]->(Symptom)`
- `(Error)-[:MAY_CAUSED_BY]->(Cause)`
- `(Cause)-[:SOLVED_BY]->(Solution)`
- `(Solution)-[:CHECK_FILE]->(File)`
- `(Solution)-[:USE_COMMAND]->(Command)`

## 关键文件

- `backend/src/main/java/com/mistmoe/troubleshoot/kg/KgController.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/kg/KgService.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/kg/GraphNode.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/kg/GraphLink.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/kg/GraphResponse.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/kg/KgImportResponse.java`
- `backend/src/test/java/com/mistmoe/troubleshoot/kg/KgControllerTest.java`
- `frontend/src/App.vue`
- `frontend/src/styles.css`
- `database/neo4j_init.cypher`

## 验证记录

前端构建：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

结果：Vite build 成功。

后端测试：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$repo='C:\Users\97780\.m2\repository'
..\tools\apache-maven-3.9.16\bin\mvn.cmd "-Dmaven.repo.local=$repo" -o test
```

结果：

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Neo4j 连通性验证：

```text
RETURN 1 AS ok
```

结果：Neo4j HTTP API 返回 `ok = 1`。

Neo4j 真实导入验证：

```text
imported=12
nodes=91
relationships=96
```

## 调试记录

- Maven 实际运行用户是 `CodexSandboxOffline`，默认本地仓库缺少 Spring Boot parent POM，导致离线测试失败。
- 改为显式指定用户 Maven 仓库 `C:\Users\97780\.m2\repository` 后，测试可以读取已有依赖。
- 读取该仓库 jar 需要提升权限，否则 Windows 会对部分 jar 报 `AccessDeniedException`。
- `spring-boot:start` 离线缺少插件传递依赖，因此真实 Neo4j 验证使用 Neo4j HTTP Transaction API 直接完成。

## 后续衔接

Phase 3 智能排障主流程可以直接复用：

- MySQL `error_case.keyword` 做关键词匹配。
- 匹配到的 `error_case.id` 传给 `GET /api/kg/graph/{errorId}`。
- 将返回的 `nodes` 和 `links` 合并进 `/api/error/analyze` 响应。
- 把用户输入、命中故障、图谱结果和后续 AI 建议写入 `analysis_history`。

## 当前限制

- 当前后端 Neo4j 接入使用 HTTP API，不使用 Neo4j Java Driver。
- 图谱搜索目前只返回匹配节点，不展开邻居关系；Phase 3 或 Phase 5 可以增强为搜索后展开一跳关系。
- 前端 ECharts 已能展示图谱，但还没有节点详情抽屉，后续演示优化阶段可补。
