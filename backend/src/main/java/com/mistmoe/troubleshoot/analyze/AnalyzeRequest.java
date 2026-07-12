package com.mistmoe.troubleshoot.analyze;

public record AnalyzeRequest(
        String inputText,
        String techStack,
        Long userId
) {
}
