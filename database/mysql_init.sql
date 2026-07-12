INSERT INTO sys_role (role_code, role_name, description) VALUES
('ADMIN', '系统管理员', '管理用户、角色、系统配置和全部数据'),
('MAINTAINER', '知识库维护员', '维护故障知识库和图谱数据'),
('USER', '普通用户', '提交报错分析并查看个人分析历史和报告')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description);

INSERT INTO sys_user (username, password, nickname, email, status) VALUES
('admin', '123456', '系统管理员', 'admin@example.com', 1),
('maintainer', '123456', '知识库维护员', 'maintainer@example.com', 1),
('user01', '123456', '普通用户', 'user01@example.com', 1)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    nickname = VALUES(nickname),
    email = VALUES(email),
    status = VALUES(status);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
INNER JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
INNER JOIN sys_role r ON r.role_code = 'MAINTAINER'
WHERE u.username = 'maintainer';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
INNER JOIN sys_role r ON r.role_code = 'USER'
WHERE u.username = 'user01';

INSERT INTO error_case (
    error_name, stage, component, keyword, symptom, cause, solution,
    related_file, related_command, risk_tip, create_user_id, update_user_id
) VALUES
('端口被占用', '后端启动', 'Spring Boot', 'Port 8080 already in use,Web server failed to start',
 'Spring Boot 启动失败，提示端口已被占用',
 '默认端口 8080 已被其他进程占用',
 '关闭占用进程，或在 application.yml 中修改 server.port',
 'application.yml', 'netstat -ano | findstr 8080', '结束进程前确认不是重要服务', 2, 2),
('Maven 依赖无法解析', '依赖管理', 'Maven', 'Cannot resolve dependency,Could not find artifact',
 '项目构建时无法下载或解析依赖',
 '仓库配置错误、依赖版本不存在、网络代理异常或本地缓存损坏',
 '检查 pom.xml 和 settings.xml，刷新 Maven，必要时清理本地缓存',
 'pom.xml,settings.xml', 'mvn clean install -U', '清理本地仓库前注意不要误删其他项目依赖', 2, 2),
('MySQL 连接失败', '数据库连接', 'MySQL', 'Communications link failure,Connection refused',
 '后端启动或调用接口时无法连接 MySQL',
 'MySQL 服务未启动、端口错误、连接地址错误或防火墙拦截',
 '确认 MySQL 服务状态，检查 spring.datasource.url、端口和数据库名',
 'application.yml', 'mysql -h localhost -P 3306 -u root -p', '不要把真实数据库密码提交到仓库', 2, 2),
('MySQL 账号密码错误', '数据库连接', 'MySQL', 'Access denied for user,using password: YES',
 'MySQL 拒绝当前用户登录',
 '用户名或密码错误，或账号没有对应主机的访问权限',
 '重新确认 Navicat 连接配置和 application.yml 中的用户名密码',
 'application.yml', 'SELECT user,host FROM mysql.user;', '修改账号权限前先确认当前连接的 MySQL 实例', 2, 2),
('数据表不存在', '数据库连接', 'MySQL', 'Table does not exist,Table doesn''t exist',
 '接口查询时报表不存在',
 '未执行建表 SQL、连接到了错误数据库，或表名大小写不一致',
 '执行 database/mysql_schema.sql，并确认 spring.datasource.url 指向 troubleshoot_kg',
 'mysql_schema.sql,application.yml', 'SHOW TABLES;', '执行建表脚本前确认当前数据库', 2, 2),
('跨域请求失败', '前后端联调', 'Spring Boot / Vue', 'CORS error,Access-Control-Allow-Origin',
 '浏览器拦截前端对后端接口的请求',
 '后端未配置 CORS，或前端请求地址和后端允许来源不一致',
 '开发阶段可使用 Vite proxy，生产阶段在后端配置明确的 CORS 来源',
 'vite.config.js,WebMvcConfigurer', 'npm run dev', '不要在生产环境开放任意来源跨域', 2, 2),
('接口 404', '接口调用', 'Spring Boot / Vue', '404 Not Found,No mapping',
 '前端调用接口返回 404',
 'axios baseURL、接口路径或 Controller 映射不一致',
 '核对前端请求路径、Vite 代理和后端 @RequestMapping',
 'vite.config.js,Controller.java', 'curl http://localhost:8088/api/health', '不要只看前端页面，要同时验证后端接口', 2, 2),
('Vite 命令不存在', '前端启动', 'Vite', 'vite is not recognized,command not found: vite',
 '运行 npm run dev 时提示 vite 不存在',
 'node_modules 未安装，或 package.json 依赖缺失',
 '执行 npm install，确认 package.json 中存在 vite 依赖',
 'package.json', 'npm.cmd install', '不要提交 node_modules 目录', 2, 2),
('npm 依赖安装失败', '前端启动', 'npm', 'npm ERR,ERESOLVE,network timeout',
 '前端依赖安装失败或版本冲突',
 '网络异常、依赖版本冲突、Node 版本不兼容或缓存损坏',
 '检查 Node 版本，清理 npm 缓存，必要时删除 node_modules 后重装',
 'package.json,package-lock.json', 'npm cache verify', '删除 lock 文件前确认团队依赖版本策略', 2, 2),
('Neo4j 认证失败', '图数据库连接', 'Neo4j', 'Unauthorized,authentication failure',
 '后端连接 Neo4j 时认证失败',
 'Neo4j 用户名或密码配置错误，或连接到了错误实例',
 '确认 Neo4j Desktop 中当前数据库、用户名和密码，检查 spring.neo4j 配置',
 'application.yml', 'MATCH (n) RETURN count(n);', '不要把生产 Neo4j 密码写入公开仓库', 2, 2),
('Neo4j 服务不可用', '图数据库连接', 'Neo4j', 'ServiceUnavailable,Connection refused',
 '后端无法连接 Neo4j Bolt 服务',
 'Neo4j 实例未启动、Bolt 端口错误或被占用',
 '启动 Neo4j Desktop 实例，确认 bolt://127.0.0.1:7687 可访问',
 'application.yml', 'neo4j status', '确认连接的是目标数据库 neo4j', 2, 2),
('大模型 API Key 无效', '外部服务调用', 'LLM API', 'invalid api key,401 Unauthorized',
 '调用大模型接口时返回认证失败',
 'API Key 未配置、写错、过期或使用了错误服务商地址',
 '检查 llm.api-key、base-url 和 model-name，确保密钥来自对应平台',
 'application.yml,.env', 'curl -H "Authorization: Bearer <key>" <base-url>', '不要在代码和提交记录中暴露真实 API Key', 2, 2)
ON DUPLICATE KEY UPDATE
    stage = VALUES(stage),
    component = VALUES(component),
    keyword = VALUES(keyword),
    symptom = VALUES(symptom),
    cause = VALUES(cause),
    solution = VALUES(solution),
    related_file = VALUES(related_file),
    related_command = VALUES(related_command),
    risk_tip = VALUES(risk_tip),
    update_user_id = VALUES(update_user_id);
