package com.mistmoe.troubleshoot.submission;

public record KnowledgeSubmissionReviewRequest(
        Long reviewerId,
        String reviewComment,
        KnowledgeSubmissionDraft draft
) {
}
