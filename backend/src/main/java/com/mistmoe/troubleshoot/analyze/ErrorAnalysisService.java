package com.mistmoe.troubleshoot.analyze;

import com.mistmoe.troubleshoot.errorcase.ErrorCase;
import com.mistmoe.troubleshoot.kg.GraphResponse;
import com.mistmoe.troubleshoot.kg.KgService;
import com.mistmoe.troubleshoot.llm.LlmAdviceService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.function.Consumer;

@Service
public class ErrorAnalysisService {
    public record Evidence(HybridCaseRetriever.Retrieval retrieval, ErrorCase best, GraphResponse graph, String graphStatus) {}
    private final HybridCaseRetriever retriever;
    private final KgService kg;
    private final AnalysisHistoryRepository history;
    private final LlmAdviceService llm;
    public ErrorAnalysisService(HybridCaseRetriever retriever, KgService kg,
            AnalysisHistoryRepository history, LlmAdviceService llm) {
        this.retriever = retriever; this.kg = kg; this.history = history; this.llm = llm;
    }
    public static void validate(AnalyzeRequest request) {
        if (request == null || request.inputText() == null || request.inputText().isBlank())
            throw new IllegalArgumentException("请填写完整的错误日志或报错信息");
        if (request.inputText().length() > 20000 || (request.techStack() != null && request.techStack().length() > 200))
            throw new IllegalArgumentException("日志最多 20000 字，技术栈最多 200 字");
    }
    public Evidence prepare(AnalyzeRequest request, Consumer<String> progress) {
        validate(request);
        progress.accept("正在检索候选案例");
        var retrieval = retriever.retrieve(request.inputText(), request.techStack());
        progress.accept("检索模式：" + retrieval.mode() + "，候选案例：" + retrieval.hits().size());
        ErrorCase best = retrieval.hits().isEmpty() ? null : retrieval.hits().get(0).errorCase();
        GraphResponse graph = new GraphResponse(List.of(), List.of());
        String graphStatus = "未命中案例";
        if (best != null) {
            try {
                graph = kg.graphByErrorId(best.id());
                graphStatus = graph.nodes().isEmpty() ? "图谱暂无关联证据" : "已检索 1～3 跳图谱证据";
            } catch (RuntimeException failure) { graphStatus = "图谱不可用，保留本地案例证据"; }
        }
        progress.accept(graphStatus);
        return new Evidence(retrieval, best, graph, graphStatus);
    }
    public AnalyzeResponse analyze(AnalyzeRequest request) {
        Evidence evidence = prepare(request, ignored -> {});
        String advice = evidence.best() == null ? fallback(evidence)
                : llm.generateAdvice(request, evidence.best(), evidence.graph());
        return save(request, evidence, advice);
    }
    public AnalyzeResponse save(AnalyzeRequest request, Evidence evidence, String advice) {
        ErrorCase c = evidence.best();
        history.save(request, c == null ? null : c.id(), c == null ? null : c.errorName(), evidence.graph(), advice);
        return new AnalyzeResponse(c != null, c == null ? null : c.id(), c == null ? null : c.errorName(),
                c == null ? null : c.stage(), c == null ? request.techStack() : c.component(),
                c == null ? List.of() : parts(c.symptom()), c == null ? List.of() : parts(c.cause()),
                c == null ? List.of() : parts(c.solution()), c == null ? List.of() : parts(c.relatedFile()),
                c == null ? List.of() : parts(c.relatedCommand()), evidence.graph(), advice,
                "检索模式：" + evidence.retrieval().mode() + "；" + evidence.graphStatus());
    }
    public static String fallback(Evidence evidence) {
        ErrorCase c = evidence.best();
        if (c == null) return "当前知识库未命中明确故障类型。请补充完整错误日志、技术栈版本、相关配置文件和复现步骤。";
        return "【本地知识库建议】\n可能原因：" + c.cause() + "\n排查步骤：" + c.solution()
                + "\n相关文件：" + c.relatedFile() + "\n排查命令：" + c.relatedCommand() + "\n风险提示：" + c.riskTip();
    }
    private List<String> parts(String value) {
        return value == null || value.isBlank() ? List.of() : java.util.Arrays.stream(value.split("[,;，；/]+"))
                .map(String::trim).filter(s -> !s.isBlank()).toList();
    }
}
