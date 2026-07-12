# CODEX HANDOFF - JavaWeb Troubleshoot KG LLM

## 0. Read This First

You are working inside the project root:

```text
javaweb-troubleshoot-kg-llm/
```

This is a course project. Do NOT build a tiny demo or a random chatbot. The system must be a coherent Java Web troubleshooting and operations assistant based on Knowledge Graph + LLM.

Project title in Chinese:

```text
基于知识图谱与大模型融合的 Java Web 项目全链路智能排障与运维辅助系统
```

Short English name:

```text
JavaWeb Troubleshoot KG LLM
```

Core idea:

```text
User submits an error message from a Java Web project.
The backend identifies the error category.
The backend retrieves structured troubleshooting knowledge from Neo4j.
The backend sends the retrieved knowledge to an LLM.
The LLM generates a structured troubleshooting suggestion.
The frontend displays the detected error, causes, solutions, related commands, related files, graph links, and history.
```

Important boundary:

```text
This is NOT an automatic bug fixer.
This is NOT a universal error solver.
This is an assistant for common Java Web development, integration, and local deployment problems.
```

## 1. Existing Local Environment

The user has already prepared the following local environment.

### 1.1 Neo4j

```text
Neo4j Desktop Studio instance: MistMoe
Neo4j database: neo4j
Neo4j username: neo4j
Neo4j password: root123456
Recommended Spring Boot URI: bolt://127.0.0.1:7687
Visible Desktop URI: neo4j://127.0.0.1:7687
```

Use this in `backend/src/main/resources/application.yml`:

```yaml
spring:
  neo4j:
    uri: bolt://127.0.0.1:7687
    authentication:
      username: neo4j
      password: root123456
```

### 1.2 MySQL

```text
MySQL database has already been created: troubleshoot_kg
```

Recommended backend config template:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/troubleshoot_kg?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: REPLACE_WITH_LOCAL_MYSQL_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver
```

Do not assume the MySQL password. Use a placeholder and ask the user to fill it in if needed.

## 2. Required Project Structure

Create and preserve this structure:

```text
javaweb-troubleshoot-kg-llm/
├── backend/
├── frontend/
├── database/
│   ├── mysql_schema.sql
│   ├── mysql_init.sql
│   └── neo4j_init.cypher
├── data/
│   └── error_cases.csv
├── docs/
│   └── CODEX_HANDOFF.md
└── README.md
```

Do not delete `database/`, `data/`, or `docs/`.

## 3. Technology Stack

Frontend:

```text
Vue 3
Vite
Element Plus
Axios
ECharts graph for simple KG visualization
```

Backend:

```text
Spring Boot
Spring Web
Spring Data JPA or MyBatis-Plus
Spring Data Neo4j or Neo4j Java Driver
MySQL
Neo4j
LLM API integration, preferably DeepSeek or Baidu Qianfan/Wenxin if the user provides working credentials
```

Databases:

```text
MySQL: users, roles, error case table, analysis history, ordinary business data
Neo4j: troubleshooting knowledge graph
```

## 4. Core Business Logic

The troubleshooting process must be:

```text
1. User enters error text.
2. User optionally selects a technical stack, such as Spring Boot, Maven, MySQL, Vue, Neo4j, or LLM API.
3. Backend matches keywords and identifies a likely Error node.
4. Backend queries Neo4j for related Stage, Component, Symptom, Cause, Solution, File, and Command nodes.
5. Backend sends the graph retrieval result to the LLM.
6. LLM generates structured advice based on the graph result.
7. Backend stores the analysis history in MySQL.
8. Frontend displays the final result and graph relationship.
```

LLM must not answer from pure imagination. It should use the Neo4j retrieval result as the main evidence.

## 5. Knowledge Graph Design

### 5.1 Node Labels

```text
Stage: troubleshooting stage, such as environment setup, dependency management, backend startup, database connection, API integration, frontend startup, frontend-backend integration, external API call
Component: technical component, such as Spring Boot, Maven, MySQL, Vue, Axios, Neo4j, DeepSeek API
Error: error type, such as port occupied, Maven dependency unresolved, database connection failed
Symptom: error symptom or keyword, such as Port 8080 already in use, CORS error, Access denied for user
Cause: possible cause
Solution: troubleshooting solution
File: related config file, such as pom.xml, application.yml, package.json, vite.config.js
Command: useful command, such as mvn clean install, npm install, netstat -ano | findstr 8080
```

### 5.2 Relationship Types

```text
(Error)-[:OCCURS_IN]->(Stage)
(Error)-[:RELATED_TO]->(Component)
(Error)-[:HAS_SYMPTOM]->(Symptom)
(Error)-[:MAY_CAUSED_BY]->(Cause)
(Cause)-[:SOLVED_BY]->(Solution)
(Solution)-[:CHECK_FILE]->(File)
(Solution)-[:USE_COMMAND]->(Command)
(Component)-[:DEPENDS_ON]->(Component)
```

### 5.3 Example Cypher

```cypher
CREATE (stage:Stage {name: '后端启动'});
CREATE (component:Component {name: 'Spring Boot'});
CREATE (err:Error {name: '端口被占用', keyword: 'Port 8080 already in use'});
CREATE (symptom:Symptom {name: '启动失败，提示端口已被占用'});
CREATE (cause:Cause {name: '8080 端口被其他进程占用'});
CREATE (solution:Solution {name: '关闭占用进程或修改 server.port'});
CREATE (file:File {name: 'application.yml'});
CREATE (cmd:Command {name: 'netstat -ano | findstr 8080'});

CREATE (err)-[:OCCURS_IN]->(stage);
CREATE (err)-[:RELATED_TO]->(component);
CREATE (err)-[:HAS_SYMPTOM]->(symptom);
CREATE (err)-[:MAY_CAUSED_BY]->(cause);
CREATE (cause)-[:SOLVED_BY]->(solution);
CREATE (solution)-[:CHECK_FILE]->(file);
CREATE (solution)-[:USE_COMMAND]->(cmd);
```

## 6. MySQL Database Design

The system must support multiple roles. This was explicitly required by the teacher.

### 6.1 Roles

```text
ADMIN: system administrator. Can manage users, roles, all history, and system configuration.
MAINTAINER: knowledge base maintainer. Can create, update, delete error cases and import graph data.
USER: normal user. Can submit error analysis requests and view personal analysis history and reports.
```

### 6.2 Required Tables

Create at least these tables:

```sql
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT 'username',
    password VARCHAR(255) NOT NULL COMMENT 'password, should be encrypted in real projects',
    nickname VARCHAR(50) COMMENT 'nickname',
    email VARCHAR(100) COMMENT 'email',
    status TINYINT DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='system user table';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'ADMIN, MAINTAINER, USER',
    role_name VARCHAR(50) NOT NULL COMMENT 'role name',
    description VARCHAR(255) COMMENT 'role description',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='system role table';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'user id',
    role_id BIGINT NOT NULL COMMENT 'role id'
) COMMENT='user-role relation table';

CREATE TABLE IF NOT EXISTS error_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    error_name VARCHAR(255) NOT NULL COMMENT 'error name',
    stage VARCHAR(100) COMMENT 'stage',
    component VARCHAR(100) COMMENT 'component',
    keyword VARCHAR(500) COMMENT 'keywords for matching',
    symptom TEXT COMMENT 'symptom',
    cause TEXT COMMENT 'possible cause',
    solution TEXT COMMENT 'solution',
    related_file VARCHAR(255) COMMENT 'related file',
    related_command VARCHAR(500) COMMENT 'related command',
    risk_tip TEXT COMMENT 'risk tip',
    create_user_id BIGINT COMMENT 'creator id',
    update_user_id BIGINT COMMENT 'updater id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='error knowledge base table';

CREATE TABLE IF NOT EXISTS analysis_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT 'submitter user id',
    input_text TEXT NOT NULL COMMENT 'original error text',
    tech_stack VARCHAR(255) COMMENT 'selected tech stack',
    matched_error_id BIGINT COMMENT 'matched error case id',
    matched_error_name VARCHAR(255) COMMENT 'matched error name',
    graph_result JSON COMMENT 'Neo4j retrieval result',
    ai_advice TEXT COMMENT 'LLM generated advice',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='analysis history table';
```

### 6.3 Initial Data

```sql
INSERT INTO sys_role (role_code, role_name, description) VALUES
('ADMIN', '系统管理员', '管理用户、角色、系统配置和全部数据'),
('MAINTAINER', '知识库维护员', '维护故障知识库和图谱数据'),
('USER', '普通用户', '提交报错分析并查看个人历史');

INSERT INTO sys_user (username, password, nickname, email, status) VALUES
('admin', '123456', '系统管理员', 'admin@example.com', 1),
('maintainer', '123456', '知识库维护员', 'maintainer@example.com', 1),
('user01', '123456', '普通用户', 'user01@example.com', 1);
```

For course demo, plain text password is acceptable only in the demo database. Add a comment in README that real projects should use BCrypt or another secure hashing method.

## 7. Frontend Pages

Required pages:

```text
1. Login page
2. Home page / dashboard
3. Intelligent troubleshooting page
4. Knowledge graph page
5. Error knowledge base management page
6. Analysis history page
7. User management page, ADMIN only
8. Role management page, ADMIN only
```

Role-based menu rules:

```text
USER:
- Home
- Intelligent troubleshooting
- My analysis history
- Knowledge graph view

MAINTAINER:
- Home
- Intelligent troubleshooting
- Error knowledge base management
- Knowledge graph management
- Analysis history

ADMIN:
- All pages
- User management
- Role management
```

Keep role control simple. This project does not require a complex permission framework.

## 8. Backend API Requirements

Minimum APIs:

```text
POST /api/auth/login
GET  /api/auth/profile
GET  /api/error/list
POST /api/error/create
PUT  /api/error/{id}
DELETE /api/error/{id}
POST /api/error/analyze
GET  /api/kg/graph/{errorId}
GET  /api/kg/search?keyword=xxx
POST /api/kg/import
GET  /api/history/list
GET  /api/history/my
GET  /api/user/list
GET  /api/role/list
```

Most important API:

```text
POST /api/error/analyze
```

Expected request:

```json
{
  "inputText": "Port 8080 already in use",
  "techStack": "Spring Boot",
  "userId": 3
}
```

Expected response:

```json
{
  "errorType": "端口被占用",
  "stage": "后端启动",
  "component": "Spring Boot",
  "causes": ["8080 端口被其他进程占用"],
  "solutions": ["关闭占用进程或修改 server.port"],
  "files": ["application.yml"],
  "commands": ["netstat -ano | findstr 8080"],
  "graph": {
    "nodes": [],
    "links": []
  },
  "aiAdvice": "..."
}
```

Graph response must be easy for ECharts graph:

```json
{
  "nodes": [
    {"id": "error-1", "name": "端口被占用", "category": "Error"}
  ],
  "links": [
    {"source": "error-1", "target": "stage-1", "label": "OCCURS_IN"}
  ]
}
```

## 9. LLM Integration

The user may use DeepSeek or Baidu Qianfan/Wenxin credentials.

There is a teacher-provided text file containing Java dependency examples for Baidu Qianfan/Wenxin. Do not hard-code real credentials into source code. Use `application.yml` placeholders or environment variables.

LLM prompt template:

```text
You are a Java Web troubleshooting assistant.

User error input:
{{inputText}}

The system retrieved the following structured knowledge from the knowledge graph:
Stage: {{stage}}
Component: {{component}}
Symptoms: {{symptoms}}
Possible causes: {{causes}}
Solutions: {{solutions}}
Related files: {{files}}
Related commands: {{commands}}

Generate a structured troubleshooting suggestion.
Rules:
1. Base the answer mainly on the retrieved knowledge graph result.
2. Do not invent unsupported solutions.
3. Give the most likely cause first.
4. Provide step-by-step actions.
5. If information is insufficient, ask for version, config file, or complete error log.
```

## 10. Development Phases

Follow phases strictly. Do not implement everything at once.

### Phase 0 - Project Skeleton Only

Goal:

```text
Create runnable frontend and backend skeletons only.
No business modules yet.
```

Tasks:

```text
1. Create backend Spring Boot project in backend/.
2. Create frontend Vue 3 + Vite project in frontend/.
3. Create README.md.
4. Create placeholder files under database/ and data/.
5. Add backend config templates for MySQL and Neo4j.
6. Add basic health check API: GET /api/health.
7. Add simple frontend home page that can call /api/health.
```

After Phase 0, report:

```text
- Files created
- Backend startup command
- Frontend startup command
- Current unfinished tasks
```

### Phase 0.5 - Multi-role Foundation

Goal:

```text
Add basic user-role model because the teacher requires multiple roles.
```

Tasks:

```text
1. Create sys_user, sys_role, sys_user_role SQL scripts.
2. Create initial roles ADMIN, MAINTAINER, USER.
3. Create simple login API.
4. Return user profile and role in login response.
5. Add frontend login page.
6. Add role-based menu visibility.
```

Do not build complex JWT or Spring Security unless necessary. Simple session/local token simulation is enough for course demo.

### Phase 1 - Error Knowledge Base

Goal:

```text
Create MySQL-backed error knowledge base CRUD.
```

Tasks:

```text
1. Create error_case table.
2. Add list/create/update/delete APIs.
3. Add frontend table page.
4. Add at least 10 seed error cases.
```

### Phase 2 - Neo4j Knowledge Graph

Goal:

```text
Create and query troubleshooting knowledge graph.
```

Tasks:

```text
1. Create neo4j_init.cypher.
2. Add Stage, Component, Error, Symptom, Cause, Solution, File, Command nodes.
3. Add relationships.
4. Add graph query APIs.
5. Return nodes and links for frontend ECharts graph.
```

### Phase 3 - Error Analysis Flow

Goal:

```text
Implement POST /api/error/analyze.
```

Tasks:

```text
1. Match input text with error keywords.
2. Retrieve related graph knowledge from Neo4j.
3. Build structured result.
4. Store analysis history in MySQL.
5. Display result on frontend.
```

### Phase 4 - LLM Advice Generation

Goal:

```text
Use LLM to generate final structured troubleshooting advice based on graph result.
```

Tasks:

```text
1. Add LLM config placeholders.
2. Add LLM client service.
3. Use graph retrieval result as prompt context.
4. Add fallback text if LLM call fails.
```

### Phase 5 - Polish and Demo

Goal:

```text
Make the system presentable for course demo.
```

Tasks:

```text
1. Improve frontend layout.
2. Add history detail page.
3. Add graph visualization.
4. Add demo data.
5. Update README with startup steps.
```

## 11. Validation Criteria

A phase is complete only if:

```text
1. Backend can start.
2. Frontend can start.
3. README startup command is updated.
4. Any new SQL or Cypher script is placed under database/.
5. No real LLM secret key is hard-coded.
6. The response reports changed files and how to verify.
```

## 12. First Task for Codex

Start with Phase 0 only.

Do not implement business features yet.

After completing Phase 0, reply with:

```text
1. Files created or modified.
2. Backend startup command.
3. Frontend startup command.
4. How to test GET /api/health.
5. What remains unfinished.
```
