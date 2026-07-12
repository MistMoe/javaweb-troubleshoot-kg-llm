package com.mistmoe.troubleshoot.submission;

public record KnowledgeSubmissionDraft(
        String errorName,
        String keyword,
        String symptom,
        String cause,
        String solution,
        String relatedFile,
        String relatedCommand,
        String riskTip
) {
}
