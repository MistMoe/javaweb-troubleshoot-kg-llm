package com.mistmoe.troubleshoot.errorcase;

public record ErrorCaseRequest(
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
        Long userId
) {
}
