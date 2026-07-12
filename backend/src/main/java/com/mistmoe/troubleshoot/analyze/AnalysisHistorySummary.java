package com.mistmoe.troubleshoot.analyze;

import java.time.LocalDateTime;

public record AnalysisHistorySummary(
        Long id,
        Long userId,
        String username,
        String nickname,
        String inputText,
        String techStack,
        Long matchedErrorId,
        String matchedErrorName,
        boolean matched,
        String aiAdvice,
        int graphNodeCount,
        int graphLinkCount,
        LocalDateTime createTime
) {
}
