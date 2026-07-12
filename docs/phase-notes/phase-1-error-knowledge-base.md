# Phase 1 Error Knowledge Base

## 本阶段目标

实现 MySQL 驱动的故障知识库模块，为后续 Neo4j 知识图谱导入和智能排障关键词匹配提供结构化数据来源。

## 已完成内容

- 新增 `error_case` 表结构。
- 初始化 12 条常见 Java Web 故障案例。
- 后端实现故障知识库 CRUD 接口。
- 前端将“故障知识库”占位页替换为可用表格页。
- 支持关键词查询、组件筛选、环节筛选。
- 支持新增、编辑、删除故障案例。
- 补充 H2 测试数据和接口测试。
- 将 Maven 编译和测试资源编码显式设置为 UTF-8。

## 关键文件

- `database/mysql_schema.sql`: 追加 `error_case` 表。
- `database/mysql_init.sql`: 追加 12 条故障案例种子数据。
- `data/error_cases.csv`: 记录部分 CSV 格式案例，便于后续扩展导入。
- `backend/src/main/java/com/mistmoe/troubleshoot/errorcase/ErrorCase.java`: 故障案例响应模型。
- `backend/src/main/java/com/mistmoe/troubleshoot/errorcase/ErrorCaseRequest.java`: 新增和更新请求模型。
- `backend/src/main/java/com/mistmoe/troubleshoot/errorcase/ErrorCaseRepository.java`: JDBC 数据访问。
- `backend/src/main/java/com/mistmoe/troubleshoot/errorcase/ErrorCaseService.java`: 业务校验。
- `backend/src/main/java/com/mistmoe/troubleshoot/errorcase/ErrorCaseController.java`: REST 接口。
- `backend/src/test/java/com/mistmoe/troubleshoot/errorcase/ErrorCaseControllerTest.java`: CRUD 接口测试。
- `frontend/src/App.vue`: 故障知识库表格、筛选栏和编辑弹窗。
- `frontend/src/styles.css`: 知识库页面布局样式。

## 后端接口

查询列表：

```http
GET /api/error/list?keyword=Port&component=Spring Boot&stage=后端启动
```

新增：

```http
POST /api/error/create
Content-Type: application/json
```

更新：

```http
PUT /api/error/{id}
Content-Type: application/json
```

删除：

```http
DELETE /api/error/{id}
```

## 初始故障案例范围

- 端口被占用
- Maven 依赖无法解析
- MySQL 连接失败
- MySQL 账号密码错误
- 数据表不存在
- 跨域请求失败
- 接口 404
- Vite 命令不存在
- npm 依赖安装失败
- Neo4j 认证失败
- Neo4j 服务不可用
- 大模型 API Key 无效

## 验证记录

后端测试：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
..\tools\apache-maven-3.9.16\bin\mvn.cmd test
```

结果：

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

前端构建：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

结果：Vite 构建成功。仍存在 chunk 体积提示，当前阶段不影响运行。

MySQL 初始化：

```powershell
$env:MYSQL_PWD='123456'
D:\DevTools\mysql-8.0.45-winx64\bin\mysql.exe --default-character-set=utf8mb4 -h localhost -P 3306 -u root troubleshoot_kg
```

已执行：

```sql
source D:/javaweb-troubleshoot-kg-llm/database/mysql_schema.sql;
source D:/javaweb-troubleshoot-kg-llm/database/mysql_init.sql;
```

验证结果：

```text
error_case: 12
```

## 调试记录

- H2 在 `RETURN_GENERATED_KEYS` 下会返回 `id/create_time/update_time` 多个键，不能直接用 `GeneratedKeyHolder#getKey()`。已改为优先从 `id` 或 `ID` 键读取主键。
- Windows 默认编码可能影响测试资源和中文断言。已在 `pom.xml` 中设置 `project.build.sourceEncoding=UTF-8`，并在 `application-test.yml` 中设置 `spring.sql.init.encoding=UTF-8`。

## 后续衔接

Phase 2 可以基于当前 `error_case` 数据生成 Neo4j 图谱：

- `error_name` -> `Error` 节点。
- `stage` -> `Stage` 节点。
- `component` -> `Component` 节点。
- `symptom` -> `Symptom` 节点。
- `cause` -> `Cause` 节点。
- `solution` -> `Solution` 节点。
- `related_file` -> `File` 节点。
- `related_command` -> `Command` 节点。

建议关系：

- `(Error)-[:OCCURS_IN]->(Stage)`
- `(Error)-[:RELATED_TO]->(Component)`
- `(Error)-[:HAS_SYMPTOM]->(Symptom)`
- `(Error)-[:MAY_CAUSED_BY]->(Cause)`
- `(Cause)-[:SOLVED_BY]->(Solution)`
- `(Solution)-[:CHECK_FILE]->(File)`
- `(Solution)-[:USE_COMMAND]->(Command)`

## 当前限制

- 目前权限只通过前端菜单做演示级控制，后端 CRUD 接口暂未强制校验角色。
- 当前删除为物理删除；课程演示足够，真实系统可改为软删除。
- `error_case` 使用 `error_name` 唯一约束，便于重复执行初始化脚本时更新种子数据。
