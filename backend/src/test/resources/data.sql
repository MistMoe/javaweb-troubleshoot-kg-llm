INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN', '系统管理员', '管理用户、角色、系统配置和全部数据'),
(2, 'MAINTAINER', '知识库维护员', '维护故障知识库和图谱数据'),
(3, 'USER', '普通用户', '提交报错分析并查看个人历史');

INSERT INTO sys_user (id, username, password, nickname, email, status) VALUES
(1, 'admin', '123456', '系统管理员', 'admin@example.com', 1),
(2, 'maintainer', '123456', '知识库维护员', 'maintainer@example.com', 1),
(3, 'user01', '123456', '普通用户', 'user01@example.com', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3);

INSERT INTO error_case (
    error_name, stage, component, keyword, symptom, cause, solution,
    related_file, related_command, risk_tip, create_user_id, update_user_id
) VALUES
('端口被占用', '后端启动', 'Spring Boot', 'Port 8080 already in use,Web server failed to start',
 'Spring Boot 启动失败，提示端口已被占用',
 '默认端口 8080 已被其他进程占用',
 '关闭占用进程，或在 application.yml 中修改 server.port',
 'application.yml', 'netstat -ano | findstr 8080', '结束进程前确认不是重要服务', 1, 1),
('Maven 依赖无法解析', '依赖管理', 'Maven', 'Cannot resolve dependency,Could not find artifact',
 '项目构建时无法下载或解析依赖',
 '仓库配置错误、依赖版本不存在、网络代理异常或本地缓存损坏',
 '检查 pom.xml 和 settings.xml，刷新 Maven，必要时清理本地缓存',
 'pom.xml', 'mvn clean install -U', '清理本地仓库前注意不要误删其他项目依赖', 2, 2);
