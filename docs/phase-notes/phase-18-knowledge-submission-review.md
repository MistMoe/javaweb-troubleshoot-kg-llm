# 阶段 18：故障经验投稿与审核入库

## 本阶段完成内容

本阶段在“故障知识库”中补充了一个轻量但完整的知识补充链路：用户可以把真实报错日志、技术栈、发生环节、修复过程、相关文件和排查命令提交为“排障经验投稿”。系统会调用后端 DeepSeek 能力整理候选案例字段，前端展示为可编辑预览；提交后进入待审核队列。

审核权限按当前角色体系开放给 `ADMIN` 和 `MAINTAINER`。审核人可以在故障知识库页打开“待审核”面板，查看投稿原文、编辑 AI 整理后的故障名称、关键词、现象、原因、方案、文件、命令和风险提示。审核通过后，系统会把候选案例写入已有 `error_case` 表，成为正式故障知识库案例；审核驳回时会保存驳回原因。

该功能没有新建独立页面，而是融入故障知识库页，因为它本质上是“知识维护入口”。后续个人资料页可以基于 `submit_user_id` 展示“我的投稿”，收藏页也可以基于通过后的 `approved_error_case_id` 关联正式案例。

## 涉及文件

- `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmission.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionRequest.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionDraft.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionReviewRequest.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionRepository.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionService.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionController.java`
- `backend/src/main/java/com/mistmoe/troubleshoot/llm/LlmAdviceService.java`
- `backend/src/test/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionControllerTest.java`
- `backend/src/test/resources/schema.sql`
- `database/mysql_schema.sql`
- `docs/sql/2026-07-10-error-case-submission.sql`
- `frontend/src/views/knowledge/KnowledgeBaseView.vue`
- `frontend/src/views/knowledge/knowledge-base-view.css`

## 新增接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/submissions/draft` | 根据用户原始经验生成候选知识库案例字段 |
| `POST` | `/api/submissions/create` | 提交排障经验，状态为待审核 |
| `GET` | `/api/submissions/list` | 查询投稿列表，可按 `status` 或 `submitUserId` 过滤 |
| `GET` | `/api/submissions/{id}` | 查看单条投稿详情 |
| `PUT` | `/api/submissions/{id}/approve` | `ADMIN` 或 `MAINTAINER` 审核通过，并写入 `error_case` |
| `PUT` | `/api/submissions/{id}/reject` | `ADMIN` 或 `MAINTAINER` 驳回投稿并保存原因 |

## 数据库衔接

新增 MySQL 表 `error_case_submission`，用于保存投稿、候选字段、审核状态、审核人和通过后生成的正式案例 ID。通过审核后的数据会写入已有 `error_case` 表，因此后续仍沿用原来的“从 MySQL 同步到 Neo4j”能力，不需要为 Neo4j 单独设计一套投稿导入逻辑。

如果本机数据库已经初始化过，需要额外执行：

```sql
source D:/javaweb-troubleshoot-kg-llm/docs/sql/2026-07-10-error-case-submission.sql;
```

如果是重新初始化数据库，执行 `database/mysql_schema.sql` 时会自动包含这张表。

## 演示衔接

建议演示顺序：

1. 进入“故障知识库”页。
2. 点击“投稿经验”，粘贴一段报错日志和自己的修复过程。
3. 点击“AI 整理候选案例”，展示 DeepSeek 将非结构化经验整理为知识库字段。
4. 提交审核。
5. 使用 `admin` 或 `maintainer` 打开“待审核”面板。
6. 修改候选字段后点击“通过入库”。
7. 在故障案例列表中看到新案例。
8. 点击“同步到 Neo4j”，说明新增案例也能进入知识图谱。

## 后续可扩展

- 个人资料页：展示“我的投稿”“我的通过率”“最近提交经验”。
- 故障案例收藏页：收藏正式 `error_case` 案例，和投稿通过后的 `approved_error_case_id` 关联。
- 审核通知：投稿通过或驳回后在顶部通知栏提醒用户。
- 更细权限：后续可以让 `USER` 只提交和查看自己的投稿，`MAINTAINER` 审核知识库，`ADMIN` 管理全部数据。

## 验证结果

已通过后端投稿审核接口测试：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\backend
$env:JAVA_HOME='D:\db\jdk17.0.19_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
..\tools\apache-maven-3.9.16\bin\mvn.cmd "-Dmaven.repo.local=C:\Users\97780\.m2\repository" "-Dtest=KnowledgeSubmissionControllerTest" test
```

已通过前端构建：

```powershell
cd D:\javaweb-troubleshoot-kg-llm\frontend
npm.cmd run build
```
