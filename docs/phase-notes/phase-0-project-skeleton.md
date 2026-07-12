# Phase 0 Project Skeleton

## 本阶段目标

创建可启动的前后端项目骨架，不实现登录、知识库、图谱、分析历史或 LLM 等业务模块。

## 已完成内容

- 创建 Spring Boot 后端最小项目。
- 创建 Vue 3 + Vite 前端最小项目。
- 添加后端健康检查接口：`GET /api/health`。
- 添加前端首页，并在页面加载时调用 `/api/health`。
- 创建 `database/` 和 `data/` 下的占位文件。
- 创建 `README.md`，记录当前启动方式。
- 创建 `.gitignore`，忽略构建产物、本地配置和依赖目录。

## 关键文件

- `backend/pom.xml`: 后端 Maven 项目定义。
- `backend/src/main/java/com/mistmoe/troubleshoot/JavawebTroubleshootApplication.java`: Spring Boot 启动类。
- `backend/src/main/java/com/mistmoe/troubleshoot/HealthController.java`: 健康检查接口。
- `backend/src/main/resources/application.yml`: Phase 0 本地开发配置。
- `backend/src/main/resources/application.example.yml`: 数据库和 LLM 配置模板。
- `frontend/src/App.vue`: Phase 0 首页。
- `frontend/vite.config.js`: Vite 配置和 `/api` 代理。
- `database/mysql_schema.sql`: MySQL 结构脚本占位。
- `database/mysql_init.sql`: MySQL 初始化数据占位。
- `database/neo4j_init.cypher`: Neo4j 初始化脚本占位。
- `data/error_cases.csv`: 故障案例 CSV 占位。

## 验证方式

后端：

```bash
cd backend
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8088/api/health
```

前端：

```bash
cd frontend
npm install
npm run dev
```

访问 Vite 输出的本地地址，首页应显示系统标题，并能看到后端健康状态。

## 本次验证记录

- `node --version`: 已确认 Node.js 可用，版本为 `v24.15.0`。
- `npm.cmd --version`: 已确认 npm 可用，版本为 `11.12.1`。
- `npm.cmd install`: 已下载前端依赖，但命令在工具超时时间边界返回 timeout；输出显示依赖已添加并完成 audit。
- `npm.cmd run build`: 已通过，Vite 成功生成 `dist/`。
- `java -version`: 已确认 Java 17 可用，版本为 Corretto `17.0.19`。
- 系统 PATH 未找到全局 `mvn` / `mvn.cmd`。
- 已下载 Apache Maven `3.9.16` 便携版到 `tools/apache-maven-3.9.16`。
- 直接运行 Maven 时曾读取到旧 Java 7，报 `Unsupported major.minor version 52.0`；临时设置 `JAVA_HOME=D:\db\jdk17.0.19_10` 后 Maven 正常识别 Java 17。
- 使用 `..\tools\apache-maven-3.9.16\bin\mvn.cmd test` 完成后端测试，结果为 `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `npm audit`: 依赖安装后提示 3 个漏洞，后续前端依赖治理阶段可处理；Phase 0 暂不升级破坏性版本。

后端测试命令：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
..\tools\apache-maven-3.9.16\bin\mvn.cmd test
```

## 后续衔接

Phase 0.5 应从当前骨架继续：

- 在 `database/mysql_schema.sql` 中追加 `sys_user`、`sys_role`、`sys_user_role`。
- 在 `database/mysql_init.sql` 中追加 ADMIN、MAINTAINER、USER 角色和演示用户。
- 后端新增认证相关接口：`POST /api/auth/login`、`GET /api/auth/profile`。
- 前端新增登录页和角色菜单。
- 暂时不需要复杂 JWT 或 Spring Security，可以用课程演示级的本地 token 模拟。

## 当前限制

- 后端暂未接入 MySQL、Neo4j 和 LLM。
- 前端暂未接入路由和角色菜单。
- `mvn` 需要在本机 PATH 中可用后才能运行后端测试和启动命令。
