# GraphRAG 排障链路与验收说明

## 本次实现

主页面调用 `POST /api/error/analyze/stream`。日志最长 20000 字，技术栈最长 200 字。

1. 从 MySQL 读取案例，按关键词与可选向量检索生成候选。
2. 对候选去重排序，取最多 5 个；首选案例固定为本次结果的主案例。
3. 查询主案例 1～3 跳 Neo4j 子图，最多返回 200 条路径。图谱失败时保留 MySQL 案例。
4. LangChain4j 将本次证据快照封装为 `searchCases`、`queryGraph` 两个只读工具，模型按需调用；工具不接受任意 SQL、Cypher 或命令。快照避免一次回答中知识库变更造成证据前后不一致。
5. 模型增量文本直接经 SSE 转发，完成后保存一次分析历史。失败时清除不完整回答，返回本地知识库建议。

原有 `POST /api/error/analyze` 保留同步返回协议，使用相同检索与图谱降级逻辑；该兼容接口沿用原有模型调用方式。工具调用和熔断由主页面使用的流式链路提供。

## 启动与配置

沿用 README 的 MySQL、Neo4j 初始化与前后端启动方式，不需要新增 MySQL 表。已有图谱在“从 MySQL 导入图谱”时迁移到带案例归属的关系。

聊天模型继续使用 `llm.enabled`、`llm.api-url`、`llm.api-key`、`llm.model`；现有 `scripts/start-backend-deepseek.ps1` 可继续使用。

向量服务独立于聊天模型。DeepSeek 聊天接口不能用于生成文本向量。配置支持 OpenAI 兼容的 `/embeddings` 协议，可使用本地 Ollama 或托管向量服务。

PowerShell 在启动后端的同一窗口设置，例如本地已安装 Ollama 并拉取 `bge-m3` 时：

```powershell
$env:EMBEDDING_BASE_URL = 'http://127.0.0.1:11434/v1'
$env:EMBEDDING_MODEL = 'bge-m3'
```

托管服务则把 `EMBEDDING_BASE_URL` 设为其 API 根地址（保留服务要求的 `/v1` 等前缀），通过 `EMBEDDING_API_KEY` 提供密钥。不要把密钥提交到 Git。

`application.example.yml` 提供 `rag.embedding` 配置样例；现有本地配置未添加这段时也可以直接使用上述环境变量。

未配置向量地址时显示 `keyword (embedding not configured)`；向量请求失败时显示 `keyword (embedding unavailable)`；成功时显示 `hybrid`。这些状态也写入最终结果的说明。

## 检索规则与局限

- 每个命中关键词加 10 分，完整故障名命中加 6 分，已有文本命中且技术栈匹配加 3 分。
- 向量文本由故障名、组件、关键词、现象、原因和方案组成。查询使用用户日志，计算余弦相似度。
- 关键词命中或相似度达到阈值的案例进入候选池。混合分数为 `关键词命中时的 (1 + min(关键词分数,50)/50) + 达到阈值的相似度`；按总分降序、案例 ID 升序排序。
- 默认向量阈值为 0.65，可通过 `rag.embedding.min-score` 调整。阈值依赖实际向量模型，需要用标注查询校准，不能把这个分数当作诊断置信度。
- 当前案例规模较小，向量保存在单进程内存中，按案例文本更新缓存，删除案例会移除缓存。重启后重建，尚未使用持久化向量数据库。扩容时应先离线构建索引，再替换逐次扫描。
- 测试中的模拟向量用于验证召回、去重、缓存失效及降级，不代表真实语义检索质量。没有提供效果提升百分比。

## 增量同步语义

MySQL 是事实来源。点击同步后读取完整案例快照，与 Neo4j `Error.fingerprint` 对比，只重建新增或内容变化的案例；已从 MySQL 删除的案例同时清理图谱。它属于**快照差分增量同步**，不是 CDC 或实时事件同步。

实体通过唯一约束和 `MERGE` 去重。每条关系记录 `caseId`，共享原因、方案节点时仍能分别更新案例的关系。更新时清理旧关系并重建，删除后清理孤立的 7 类辅助实体。图谱查询按 `caseId` 过滤，防止共享原因节点把其他案例的解决方案误当作当前证据。

约束创建独立提交；案例与关系变更在一个 Neo4j 事务内提交，失败则整体回滚。方法对单进程内的同步任务串行执行。多实例部署应增加跨实例协调；当前设计不宣称分布式一致性。

图谱数据库应专用于本系统的这 8 类实体。孤立辅助节点清理和旧关系迁移依赖这一约定，不应与其他应用共用同名节点标签。

## 时间预算、熔断和取消

- 流式任务线程数 4、等待队列 8，满载返回 HTTP 503。
- 单次向量请求超时 5 秒且不重试；案例向量批量生成，查询向量单独生成。
- Neo4j 连接和读取超时均为 5 秒。
- 流式任务从接收起预算 60 秒；模型等待不超过剩余预算与 45 秒的较小值。SSE 超时为 65 秒，浏览器保护超时为 70 秒。
- 最多实际执行 6 次工具调用，同时限制 LangChain4j 的连续工具轮次；超出后降级。
- 模型连续失败 3 次后熔断 30 秒；恢复期只允许一个探测请求。成功复位，探测失败再次打开。
- 浏览器“停止分析”或离开页面会中断请求。服务端检测到断连后停止向页面发送结果、取消工作线程并阻止后续工具调用。底层模型连接的释放取决于 SDK 和读超时，不能保证外部服务立即停止计费。
- 已开始的数据库提交不能因客户端恰好断开而撤销，因此停止后偶尔仍可能存在一条已完成历史。

## SSE 协议

事件包括 `progress`、`retrieval`、`tool`、`token`、`reset`、`result`、`failure`。数据统一使用 JSON 包装 `{"value": ...}`，避免文本转换器把字符串作为未加引号的 JSON 输出。

`token` 是模型真实增量文本；`reset` 表示丢弃已显示的部分文本；只有 `result` 表示业务结果完整，前端随后允许导出。无模型或降级时直接返回本地结果，不模拟打字动画。

反向代理需关闭 SSE 缓冲，并将读取超时设为大于 65 秒。服务端已设置 `X-Accel-Buffering: no` 和 `Cache-Control: no-cache, no-transform`。

## 验证命令

```powershell
$env:JAVA_HOME = 'D:\db\jdk17.0.19_10'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\tools\apache-maven-3.9.16\bin\mvn.cmd -f backend/pom.xml test
```

```powershell
cd frontend
npm.cmd run test:ui
npm.cmd run test:sse
npm.cmd run build
```

`KgSyncIntegrationTest` 使用独立内存 Neo4j 引擎执行真实 Cypher，覆盖共享实体、重复同步、更新、删除、旧图迁移和失败回滚，不连接本地 Neo4j。`TroubleshootAgentTest` 使用本地 HTTP 模拟模型服务，验证两轮实际工具调用、中文 token、错误降级和熔断。接口测试覆盖 SSE 正常完成、无匹配案例、图谱不可用及空输入。

现场演示至少检查：原始英文报错、中文同义改写、无关问题、向量服务离线、Neo4j 离线、模型失败和停止分析。只有真实向量服务启用后，中文同义改写的结果才有语义检索评估价值。

## 面试表述

2026-11-02 本地验证：后端 45 项测试通过（0 failures、0 errors、0 skipped），前端 UI 契约、SSE 分包测试及生产构建通过。模型 HTTP 协议使用本地模拟服务；真实 DeepSeek、真实向量服务及实际检索质量尚未联调，需按上述配置单独验收。

可以解释为何小数据量仍引入向量：为了比较关键词与语义召回的覆盖差异，并学习检索、工具调用、流式交互和异常控制的工程链路。不能据此声称向量一定更好。

不要把固定的 31 项测试数写回简历，测试数量会变化；不要把当前系统描述为已经证明高可用。简历中的“增量同步”应能解释上述快照差分方式及其边界。

参考：[LangChain4j 工具调用文档](https://docs.langchain4j.dev/tutorials/tools/)、[所用版本 SSE UTF-8 解析实现](https://github.com/langchain4j/langchain4j/blob/1.10.0/langchain4j-http-client/src/main/java/dev/langchain4j/http/client/sse/DefaultServerSentEventParser.java)。
