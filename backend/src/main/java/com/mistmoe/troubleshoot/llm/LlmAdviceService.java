package com.mistmoe.troubleshoot.llm;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mistmoe.troubleshoot.analyze.AnalyzeRequest;
import com.mistmoe.troubleshoot.errorcase.ErrorCase;
import com.mistmoe.troubleshoot.kg.GraphLink;
import com.mistmoe.troubleshoot.kg.GraphNode;
import com.mistmoe.troubleshoot.kg.GraphResponse;
import com.mistmoe.troubleshoot.submission.KnowledgeSubmissionDraft;
import com.mistmoe.troubleshoot.submission.KnowledgeSubmissionRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class LlmAdviceService {

    private final LlmProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmAdviceService(LlmProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public String generateAdvice(AnalyzeRequest request, ErrorCase errorCase, GraphResponse graph) {
        String fallback = fallbackAdvice(errorCase);
        if (!properties.isEnabled()) {
            return fallback;
        }

        if ("qianfan".equals(normalize(properties.getProvider()))) {
            return generateQianfanAdvice(request, errorCase, graph, fallback);
        }

        if (!hasText(properties.getApiKey()) || !hasText(properties.getApiUrl())) {
            return fallback;
        }

        try {
            JsonNode response = restClient.post()
                    .uri(properties.getApiUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(openAiCompatibleRequestBody(request, errorCase, graph))
                    .retrieve()
                    .body(JsonNode.class);
            String content = response == null
                    ? ""
                    : response.path("choices").path(0).path("message").path("content").asText("");
            return hasText(content) ? content.trim() : fallback;
        } catch (RuntimeException exception) {
            return fallbackWithFailureNote(fallback);
        }
    }

    public String generateAssistantReply(String message) {
        String fallback = fallbackAssistantReply(message);
        if (!properties.isEnabled()) {
            return fallback;
        }

        if ("qianfan".equals(normalize(properties.getProvider()))) {
            return generateQianfanAssistantReply(message, fallback);
        }

        if (!hasText(properties.getApiKey()) || !hasText(properties.getApiUrl())) {
            return fallback;
        }

        try {
            JsonNode response = restClient.post()
                    .uri(properties.getApiUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(openAiCompatibleAssistantBody(message))
                    .retrieve()
                    .body(JsonNode.class);
            String content = response == null
                    ? ""
                    : response.path("choices").path(0).path("message").path("content").asText("");
            return hasText(content) ? content.trim() : fallback;
        } catch (RuntimeException exception) {
            return fallbackWithFailureNote(fallback);
        }
    }

    public KnowledgeSubmissionDraft generateCaseDraft(KnowledgeSubmissionRequest request) {
        KnowledgeSubmissionDraft fallback = fallbackCaseDraft(request);
        if (!properties.isEnabled()) {
            return fallback;
        }

        if ("qianfan".equals(normalize(properties.getProvider()))) {
            return generateQianfanCaseDraft(request, fallback);
        }

        if (!hasText(properties.getApiKey()) || !hasText(properties.getApiUrl())) {
            return fallback;
        }

        try {
            JsonNode response = restClient.post()
                    .uri(properties.getApiUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(openAiCompatibleCaseDraftBody(request))
                    .retrieve()
                    .body(JsonNode.class);
            String content = response == null
                    ? ""
                    : response.path("choices").path(0).path("message").path("content").asText("");
            return parseCaseDraft(content, fallback);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String generateQianfanAdvice(AnalyzeRequest request, ErrorCase errorCase, GraphResponse graph, String fallback) {
        if (!hasText(properties.getQianfanAccessKey())
                || !hasText(properties.getQianfanSecretKey())
                || !hasText(properties.getQianfanTokenUrl())
                || !hasText(properties.getQianfanChatUrl())
                || !hasText(properties.getQianfanModel())) {
            return fallback;
        }

        try {
            String accessToken = fetchQianfanAccessToken();
            if (!hasText(accessToken)) {
                return fallback;
            }
            String chatUrl = properties.getQianfanChatUrl()
                    .replace("{model}", properties.getQianfanModel());
            JsonNode response = restClient.post()
                    .uri(UriComponentsBuilder.fromUriString(chatUrl)
                            .queryParam("access_token", accessToken)
                            .build()
                            .toUri())
                    .body(qianfanRequestBody(request, errorCase, graph))
                    .retrieve()
                    .body(JsonNode.class);
            String content = extractQianfanContent(response);
            return hasText(content) ? content.trim() : fallback;
        } catch (RuntimeException exception) {
            return fallbackWithFailureNote(fallback);
        }
    }

    private String generateQianfanAssistantReply(String message, String fallback) {
        if (!hasText(properties.getQianfanAccessKey())
                || !hasText(properties.getQianfanSecretKey())
                || !hasText(properties.getQianfanTokenUrl())
                || !hasText(properties.getQianfanChatUrl())
                || !hasText(properties.getQianfanModel())) {
            return fallback;
        }

        try {
            String accessToken = fetchQianfanAccessToken();
            if (!hasText(accessToken)) {
                return fallback;
            }
            String chatUrl = properties.getQianfanChatUrl()
                    .replace("{model}", properties.getQianfanModel());
            JsonNode response = restClient.post()
                    .uri(UriComponentsBuilder.fromUriString(chatUrl)
                            .queryParam("access_token", accessToken)
                            .build()
                            .toUri())
                    .body(qianfanAssistantBody(message))
                    .retrieve()
                    .body(JsonNode.class);
            String content = extractQianfanContent(response);
            return hasText(content) ? content.trim() : fallback;
        } catch (RuntimeException exception) {
            return fallbackWithFailureNote(fallback);
        }
    }

    private KnowledgeSubmissionDraft generateQianfanCaseDraft(KnowledgeSubmissionRequest request, KnowledgeSubmissionDraft fallback) {
        if (!hasText(properties.getQianfanAccessKey())
                || !hasText(properties.getQianfanSecretKey())
                || !hasText(properties.getQianfanTokenUrl())
                || !hasText(properties.getQianfanChatUrl())
                || !hasText(properties.getQianfanModel())) {
            return fallback;
        }

        try {
            String accessToken = fetchQianfanAccessToken();
            if (!hasText(accessToken)) {
                return fallback;
            }
            String chatUrl = properties.getQianfanChatUrl()
                    .replace("{model}", properties.getQianfanModel());
            JsonNode response = restClient.post()
                    .uri(UriComponentsBuilder.fromUriString(chatUrl)
                            .queryParam("access_token", accessToken)
                            .build()
                            .toUri())
                    .body(qianfanCaseDraftBody(request))
                    .retrieve()
                    .body(JsonNode.class);
            return parseCaseDraft(extractQianfanContent(response), fallback);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String fetchQianfanAccessToken() {
        JsonNode response = restClient.get()
                .uri(UriComponentsBuilder.fromUriString(properties.getQianfanTokenUrl())
                        .queryParam("grant_type", "client_credentials")
                        .queryParam("client_id", properties.getQianfanAccessKey())
                        .queryParam("client_secret", properties.getQianfanSecretKey())
                        .build()
                        .toUri())
                .retrieve()
                .body(JsonNode.class);
        return response == null ? "" : response.path("access_token").asText("");
    }

    private Map<String, Object> openAiCompatibleRequestBody(AnalyzeRequest request, ErrorCase errorCase, GraphResponse graph) {
        return Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", userPrompt(request, errorCase, graph))
                ),
                "temperature", 0.2
        );
    }

    private Map<String, Object> openAiCompatibleAssistantBody(String message) {
        return Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", assistantSystemPrompt()),
                        Map.of("role", "user", "content", nullToEmpty(message))
                ),
                "temperature", 0.2
        );
    }

    private Map<String, Object> openAiCompatibleCaseDraftBody(KnowledgeSubmissionRequest request) {
        return Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", caseDraftSystemPrompt()),
                        Map.of("role", "user", "content", caseDraftUserPrompt(request))
                ),
                "temperature", 0.15
        );
    }

    private Map<String, Object> qianfanRequestBody(AnalyzeRequest request, ErrorCase errorCase, GraphResponse graph) {
        return Map.of(
                "messages", List.of(
                        Map.of("role", "user", "content", systemPrompt() + "\n\n" + userPrompt(request, errorCase, graph))
                ),
                "temperature", 0.2
        );
    }

    private Map<String, Object> qianfanAssistantBody(String message) {
        return Map.of(
                "messages", List.of(
                        Map.of("role", "user", "content", assistantSystemPrompt() + "\n\n用户问题：\n" + nullToEmpty(message))
                ),
                "temperature", 0.2
        );
    }

    private Map<String, Object> qianfanCaseDraftBody(KnowledgeSubmissionRequest request) {
        return Map.of(
                "messages", List.of(
                        Map.of("role", "user", "content", caseDraftSystemPrompt() + "\n\n" + caseDraftUserPrompt(request))
                ),
                "temperature", 0.15
        );
    }

    private String extractQianfanContent(JsonNode response) {
        if (response == null) {
            return "";
        }
        String result = response.path("result").asText("");
        if (hasText(result)) {
            return result;
        }
        return response.path("choices").path(0).path("message").path("content").asText("");
    }

    private String systemPrompt() {
        return """
                You are a Java Web troubleshooting assistant.
                Base your answer mainly on the retrieved knowledge graph and structured case fields.
                Do not invent unsupported solutions.
                Give the most likely cause first, then step-by-step actions.
                If information is insufficient, ask for versions, config files, and complete error logs.
                """;
    }

    private String assistantSystemPrompt() {
        return """
                你是 JavaWeb Troubleshoot KG LLM 系统里的全局 AI 助手。
                请用中文回答，语气清晰、克制、适合课程演示。
                你可以帮助用户分析 Java Web 报错、解释知识图谱关系、整理排障报告。
                如果问题需要真实项目上下文，请提醒用户到“智能排障”页提交完整报错日志、技术栈和补充信息。
                回答要给出可执行步骤，不要编造系统中不存在的数据。
                """;
    }

    private String caseDraftSystemPrompt() {
        return """
                你是 JavaWeb 故障知识库维护助手。
                请把用户提交的排障经验整理成一个可入库的结构化故障案例。
                只返回 JSON，不要 Markdown，不要解释。
                JSON 字段必须包含：
                errorName, keyword, symptom, cause, solution, relatedFile, relatedCommand, riskTip。
                keyword 用逗号分隔多个匹配词。
                如果信息不足，用“需补充”表达，不要编造具体版本号或不存在的文件。
                """;
    }

    private String userPrompt(AnalyzeRequest request, ErrorCase errorCase, GraphResponse graph) {
        return """
                User error input:
                %s

                Selected tech stack:
                %s

                Retrieved knowledge:
                Error: %s
                Stage: %s
                Component: %s
                Symptom: %s
                Possible cause: %s
                Solution: %s
                Related files: %s
                Related commands: %s

                Knowledge graph:
                Nodes: %s
                Links: %s

                Please generate a structured troubleshooting suggestion in Chinese.
                """.formatted(
                nullToEmpty(request.inputText()),
                nullToEmpty(request.techStack()),
                nullToEmpty(errorCase.errorName()),
                nullToEmpty(errorCase.stage()),
                nullToEmpty(errorCase.component()),
                nullToEmpty(errorCase.symptom()),
                nullToEmpty(errorCase.cause()),
                nullToEmpty(errorCase.solution()),
                nullToEmpty(errorCase.relatedFile()),
                nullToEmpty(errorCase.relatedCommand()),
                summarizeNodes(graph),
                summarizeLinks(graph)
        );
    }

    private String caseDraftUserPrompt(KnowledgeSubmissionRequest request) {
        return """
                用户投稿信息：
                报错日志：
                %s

                技术栈：
                %s

                发生环节：
                %s

                用户解决过程：
                %s

                相关文件：
                %s

                相关命令：
                %s
                """.formatted(
                nullToEmpty(request.rawLog()),
                nullToEmpty(request.techStack()),
                nullToEmpty(request.stage()),
                nullToEmpty(request.userSolution()),
                nullToEmpty(request.relatedFile()),
                nullToEmpty(request.relatedCommand())
        );
    }

    private KnowledgeSubmissionDraft parseCaseDraft(String content, KnowledgeSubmissionDraft fallback) {
        if (!hasText(content)) {
            return fallback;
        }
        try {
            String json = content.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
            }
            JsonNode node = objectMapper.readTree(json);
            return new KnowledgeSubmissionDraft(
                    textOrFallback(node, "errorName", fallback.errorName()),
                    textOrFallback(node, "keyword", fallback.keyword()),
                    textOrFallback(node, "symptom", fallback.symptom()),
                    textOrFallback(node, "cause", fallback.cause()),
                    textOrFallback(node, "solution", fallback.solution()),
                    textOrFallback(node, "relatedFile", fallback.relatedFile()),
                    textOrFallback(node, "relatedCommand", fallback.relatedCommand()),
                    textOrFallback(node, "riskTip", fallback.riskTip())
            );
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String fallbackAdvice(ErrorCase errorCase) {
        return "当前使用知识库兜底建议：优先检查“" + nullToEmpty(errorCase.cause())
                + "”，处理方式为“" + nullToEmpty(errorCase.solution())
                + "”。如需大模型生成更完整步骤，请在配置中启用 LLM 并填写 API Key。";
    }

    private KnowledgeSubmissionDraft fallbackCaseDraft(KnowledgeSubmissionRequest request) {
        String stack = hasText(request.techStack()) ? request.techStack().trim() : "JavaWeb";
        String stage = hasText(request.stage()) ? request.stage().trim() : "排障过程";
        String raw = nullToEmpty(request.rawLog());
        String solution = hasText(request.userSolution())
                ? request.userSolution().trim()
                : "请补充完整解决过程后再入库";
        String keyword = extractKeyword(raw, stack);
        return new KnowledgeSubmissionDraft(
                stack + " " + stage + "异常",
                keyword,
                compact(raw, "用户提交了故障日志，等待维护员确认报错现象"),
                "根据用户经验，可能与 " + stack + " 配置、连接状态或运行环境有关，需维护员复核",
                solution,
                nullToEmpty(request.relatedFile()),
                nullToEmpty(request.relatedCommand()),
                "该案例来自用户投稿，入库前需确认日志、环境和解决步骤是否完整"
        );
    }

    private String fallbackAssistantReply(String message) {
        String normalized = normalize(message);
        if (normalized.contains("spring")) {
            return "当前使用本地兜底建议：Spring Boot 启动失败可以先检查端口占用、application.yml 配置、依赖冲突和完整启动日志。若要调用 DeepSeek，请使用 DeepSeek 启动脚本启动后端。";
        }
        if (message != null && (message.contains("图谱") || normalized.contains("neo4j") || normalized.contains("kg"))) {
            return "当前使用本地兜底建议：知识图谱会把故障、组件、环节、原因、方案、文件和命令连接起来。建议先查看 Error 节点，再沿 MAY_CAUSED_BY、SOLVED_BY、CHECK_FILE 等关系排查。";
        }
        if (message != null && (message.contains("报告") || normalized.contains("report"))) {
            return "当前使用本地兜底建议：排障报告可以按“报错现象、命中案例、原因链路、处理步骤、后续预防”五段整理。";
        }
        return "当前使用本地兜底建议：请提供报错现象、技术栈、关键日志和已尝试操作。若已用 DeepSeek 脚本启动后端，系统会优先尝试调用真实大模型。";
    }

    private String fallbackWithFailureNote(String fallback) {
        return fallback + "\n\nLLM 调用失败，已使用知识库兜底建议。";
    }

    private String textOrFallback(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("");
        return hasText(value) ? value.trim() : fallback;
    }

    private String extractKeyword(String rawLog, String stack) {
        String normalized = nullToEmpty(rawLog).replaceAll("\\s+", " ").trim();
        if (!normalized.isEmpty()) {
            return compact(normalized, stack);
        }
        return stack + ",用户投稿";
    }

    private String compact(String value, String fallback) {
        String text = nullToEmpty(value).replaceAll("\\s+", " ").trim();
        if (!hasText(text)) {
            return fallback;
        }
        return text.length() > 120 ? text.substring(0, 120) : text;
    }

    private String summarizeNodes(GraphResponse graph) {
        if (graph == null || graph.nodes() == null || graph.nodes().isEmpty()) {
            return "[]";
        }
        return graph.nodes().stream()
                .map(this::summarizeNode)
                .toList()
                .toString();
    }

    private String summarizeLinks(GraphResponse graph) {
        if (graph == null || graph.links() == null || graph.links().isEmpty()) {
            return "[]";
        }
        return graph.links().stream()
                .map(this::summarizeLink)
                .toList()
                .toString();
    }

    private String summarizeNode(GraphNode node) {
        return node.category() + ":" + node.name();
    }

    private String summarizeLink(GraphLink link) {
        return link.source() + "-[" + link.label() + "]->" + link.target();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty() && !value.startsWith("REPLACE_WITH");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
