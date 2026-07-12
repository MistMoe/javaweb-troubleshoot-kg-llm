package com.mistmoe.troubleshoot.errorcase;

import java.time.LocalDateTime;

public record ErrorCase(
        Long id,
        String errorName,
        String stage,
        String component,
        String keyword,
        String symptom,
        String cause,
        String solution,
        String relatedFile,
        String relatedCommand,
        String riskTip,
        Long createUserId,
        Long updateUserId,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
