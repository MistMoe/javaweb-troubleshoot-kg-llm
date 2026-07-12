package com.mistmoe.troubleshoot.analyze;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.mistmoe.troubleshoot.errorcase.ErrorCase;
import com.mistmoe.troubleshoot.errorcase.ErrorCaseRepository;
import com.mistmoe.troubleshoot.kg.GraphResponse;
import com.mistmoe.troubleshoot.kg.KgService;
import com.mistmoe.troubleshoot.llm.LlmAdviceService;
import org.springframework.stereotype.Service;

@Service
public class ErrorAnalysisService {

    private static final Pattern PART_SPLITTER = Pattern.compile("[,;，；/]+");

    private final ErrorCaseRepository errorCaseRepository;
    private final KgService kgService;
    private final AnalysisHistoryRepository historyRepository;
    private final LlmAdviceService llmAdviceService;

    public ErrorAnalysisService(ErrorCaseRepository errorCaseRepository, KgService kgService,
                                AnalysisHistoryRepository historyRepository, LlmAdviceService llmAdviceService) {
        this.errorCaseRepository = errorCaseRepository;
        this.kgService = kgService;
        this.historyRepository = historyRepository;
        this.llmAdviceService = llmAdviceService;
    }

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        if (request.inputText() == null || request.inputText().trim().isEmpty()) {
            throw new IllegalArgumentException("请填写完整的错误日志或报错信息");
        }

        ErrorCase matchedCase = findBestMatch(request);
        if (matchedCase == null) {
            GraphResponse emptyGraph = new GraphResponse(List.of(), List.of());
            String advice = "当前知识库未命中明确故障类型。请补充完整错误日志、技术栈版本、相关配置文件和复现步骤。";
            historyRepository.save(request, null, null, emptyGraph, advice);
            return new AnalyzeResponse(
                    false,
                    null,
                    null,
                    null,
                    request.techStack(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    emptyGraph,
                    advice,
                    "未匹配到知识库中的故障案例，请补充更完整的报错文本、技术栈版本和关键配置。"
            );
        }

        GraphResponse graph = kgService.graphByErrorId(matchedCase.id());
        String advice = llmAdviceService.generateAdvice(request, matchedCase, graph);
        historyRepository.save(request, matchedCase.id(), matchedCase.errorName(), graph, advice);
        return new AnalyzeResponse(
                true,
                matchedCase.id(),
                matchedCase.errorName(),
                matchedCase.stage(),
                matchedCase.component(),
                splitParts(matchedCase.symptom()),
                splitParts(matchedCase.cause()),
                splitParts(matchedCase.solution()),
                splitParts(matchedCase.relatedFile()),
                splitParts(matchedCase.relatedCommand()),
                graph,
                advice,
                "已根据错误关键字命中知识库案例，并检索 Neo4j 知识图谱。"
        );
    }

    private ErrorCase findBestMatch(AnalyzeRequest request) {
        String input = normalize(request.inputText());
        String techStack = normalize(request.techStack());
        return errorCaseRepository.list(null, null, null).stream()
                .map(item -> new ScoredErrorCase(item, score(item, input, techStack)))
                .filter(item -> item.score() > 0)
                .max(Comparator.comparingInt(ScoredErrorCase::score)
                        .thenComparing(item -> -item.errorCase().id()))
                .map(ScoredErrorCase::errorCase)
                .orElse(null);
    }

    private int score(ErrorCase item, String input, String techStack) {
        int score = 0;
        for (String keyword : splitParts(item.keyword())) {
            String normalizedKeyword = normalize(keyword);
            if (!normalizedKeyword.isEmpty() && input.contains(normalizedKeyword)) {
                score += 10;
            }
        }
        if (!normalize(item.errorName()).isEmpty() && input.contains(normalize(item.errorName()))) {
            score += 6;
        }
        if (score > 0 && !techStack.isEmpty() && normalize(item.component()).contains(techStack)) {
            score += 3;
        }
        return score;
    }

    private String buildAdvice(ErrorCase item) {
        return "当前阶段先使用知识库生成建议：优先检查“" + item.cause()
                + "”，处理方式为“" + item.solution()
                + "”。后续 Phase 4 会把该图谱检索结果交给大模型生成更完整的步骤化建议。";
    }

    private List<String> splitParts(String value) {
        if (value == null || value.trim().isEmpty()) {
            return List.of();
        }
        List<String> parts = new ArrayList<>();
        for (String part : PART_SPLITTER.split(value)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ScoredErrorCase(ErrorCase errorCase, int score) {
    }
}
