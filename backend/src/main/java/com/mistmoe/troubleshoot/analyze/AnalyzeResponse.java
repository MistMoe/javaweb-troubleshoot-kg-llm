package com.mistmoe.troubleshoot.analyze;

import java.util.List;

import com.mistmoe.troubleshoot.kg.GraphResponse;

public record AnalyzeResponse(
        boolean matched,
        Long errorId,
        String errorName,
        String stage,
        String component,
        List<String> symptoms,
        List<String> causes,
        List<String> solutions,
        List<String> files,
        List<String> commands,
        GraphResponse graph,
        String aiAdvice,
        String message
) {
}
