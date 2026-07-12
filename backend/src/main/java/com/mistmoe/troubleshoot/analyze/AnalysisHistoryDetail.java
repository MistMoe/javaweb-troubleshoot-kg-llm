package com.mistmoe.troubleshoot.analyze;

import java.time.LocalDateTime;

import com.mistmoe.troubleshoot.kg.GraphResponse;

public record AnalysisHistoryDetail(
        Long id,
        Long userId,
        String username,
        String nickname,
        String inputText,
        String techStack,
        Long matchedErrorId,
        String matchedErrorName,
        boolean matched,
        GraphResponse graph,
        String aiAdvice,
        LocalDateTime createTime
) {
}
