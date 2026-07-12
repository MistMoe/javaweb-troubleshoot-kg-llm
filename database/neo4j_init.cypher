// Phase 2 Neo4j initialization script.
// The backend endpoint POST /api/kg/import imports all rows from MySQL error_case.
// This script is useful when you want to prepare constraints or manually seed examples in Neo4j Studio.

CREATE CONSTRAINT error_mysql_id IF NOT EXISTS FOR (n:Error) REQUIRE n.mysqlId IS UNIQUE;
CREATE CONSTRAINT stage_name IF NOT EXISTS FOR (n:Stage) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT component_name IF NOT EXISTS FOR (n:Component) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT symptom_name IF NOT EXISTS FOR (n:Symptom) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT cause_name IF NOT EXISTS FOR (n:Cause) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT solution_name IF NOT EXISTS FOR (n:Solution) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT file_name IF NOT EXISTS FOR (n:File) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT command_name IF NOT EXISTS FOR (n:Command) REQUIRE n.name IS UNIQUE;

// Minimal manual seed example. The full project import should use POST /api/kg/import.
MERGE (stage:Stage {name: '后端启动'});
MERGE (component:Component {name: 'Spring Boot'});
MERGE (error:Error {mysqlId: 1})
SET error.name = '端口被占用',
    error.keyword = 'Port 8080 already in use,Web server failed to start';
MERGE (symptom:Symptom {name: 'Spring Boot 启动失败，提示端口已被占用'});
MERGE (cause:Cause {name: '默认端口 8080 已被其他进程占用'});
MERGE (solution:Solution {name: '关闭占用进程，或在 application.yml 中修改 server.port'});
MERGE (file:File {name: 'application.yml'});
MERGE (command:Command {name: 'netstat -ano | findstr 8080'});

MERGE (error)-[:OCCURS_IN]->(stage);
MERGE (error)-[:RELATED_TO]->(component);
MERGE (error)-[:HAS_SYMPTOM]->(symptom);
MERGE (error)-[:MAY_CAUSED_BY]->(cause);
MERGE (cause)-[:SOLVED_BY]->(solution);
MERGE (solution)-[:CHECK_FILE]->(file);
MERGE (solution)-[:USE_COMMAND]->(command);
