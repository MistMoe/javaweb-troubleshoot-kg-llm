# Phase 0.5 Multi-role Foundation

## 本阶段目标

增加课程项目要求的多角色基础能力，支持 ADMIN、MAINTAINER、USER 三类角色，为后续知识库维护、图谱管理、用户历史隔离做准备。

## 已完成内容

- 新增 MySQL 表脚本：`sys_user`、`sys_role`、`sys_user_role`。
- 新增初始化脚本：ADMIN、MAINTAINER、USER 三个角色和三个演示用户。
- 后端新增简单登录接口：`POST /api/auth/login`。
- 后端新增当前用户资料接口：`GET /api/auth/profile`。
- 后端使用 Spring JDBC 查询 MySQL 用户和角色。
- 前端新增登录页，提供 admin、maintainer、user01 三个演示账号快捷填充。
- 前端新增角色菜单规则，不同角色可见菜单不同。
- 继续保留 Phase 0 的后端健康检查。

## 关键文件

- `database/mysql_schema.sql`: 多角色基础表结构。
- `database/mysql_init.sql`: 多角色演示数据。
- `backend/pom.xml`: 新增 Spring JDBC、MySQL Driver、H2 测试依赖。
- `backend/src/main/resources/application.yml`: 本地 MySQL 连接配置。
- `backend/src/main/java/com/mistmoe/troubleshoot/auth/AuthController.java`: 登录和用户资料接口。
- `backend/src/main/java/com/mistmoe/troubleshoot/auth/AuthService.java`: 登录校验和简化 token 会话。
- `backend/src/main/java/com/mistmoe/troubleshoot/auth/AuthRepository.java`: 用户和角色查询。
- `backend/src/test/java/com/mistmoe/troubleshoot/auth/AuthControllerTest.java`: 登录接口测试。
- `frontend/src/App.vue`: 登录页、角色菜单和模块占位页。
- `frontend/src/styles.css`: 登录页和后台布局样式。

## 后端接口

登录：

```http
POST /api/auth/login
Content-Type: application/json
```

请求：

```json
{
  "username": "admin",
  "password": "123456"
}
```

返回：

```json
{
  "token": "demo-token",
  "user": {
    "id": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "email": "admin@example.com",
    "roles": [
      {
        "roleCode": "ADMIN",
        "roleName": "系统管理员"
      }
    ]
  }
}
```

当前用户资料：

```http
GET /api/auth/profile
Authorization: Bearer <token>
```

## 角色菜单规则

- USER: 首页、智能排障、我的分析历史、知识图谱。
- MAINTAINER: 首页、智能排障、故障知识库、知识图谱、分析历史。
- ADMIN: 所有页面，包括用户管理和角色管理。

## 验证记录

- 后端测试命令：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
..\tools\apache-maven-3.9.16\bin\mvn.cmd test
```

- 后端测试结果：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 前端构建命令：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```

- 前端构建结果：Vite build 成功；仍有 Element Plus/ECharts 相关 chunk 体积提示，当前阶段不影响运行。

## MySQL 执行情况

已使用更正后的本地 MySQL 凭据连接 `troubleshoot_kg`，并执行：

```sql
source D:/javaweb-troubleshoot-kg-llm/database/mysql_schema.sql;
source D:/javaweb-troubleshoot-kg-llm/database/mysql_init.sql;
```

验证结果：

```text
sys_user: 3
sys_role: 3
sys_user_role: 3
```

## 后续衔接

Phase 1 应继续在当前多角色基础上实现故障知识库：

- 在 `database/mysql_schema.sql` 中追加 `error_case` 表。
- 在 `database/mysql_init.sql` 中追加至少 10 条故障案例。
- 后端新增 `GET /api/error/list`、`POST /api/error/create`、`PUT /api/error/{id}`、`DELETE /api/error/{id}`。
- 前端把当前“故障知识库”占位页替换为可查询、可新增、可编辑、可删除的表格页。
- MAINTAINER 和 ADMIN 可管理知识库，USER 只保留排障和个人历史入口。

## 当前限制

- 当前 token 是内存会话，重启后失效，适合课程演示，不是生产级认证。
- 当前演示账号密码为明文，后续报告中需要说明真实项目应使用 BCrypt。
- MySQL 真实连接已验证通过，三张多角色基础表和演示数据已写入 `troubleshoot_kg`。
- 前端页面只完成角色菜单和占位模块，业务页面会在后续阶段逐步替换。
