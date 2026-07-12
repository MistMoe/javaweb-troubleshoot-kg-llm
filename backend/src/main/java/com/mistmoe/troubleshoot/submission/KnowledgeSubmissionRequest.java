package com.mistmoe.troubleshoot.submission;

public record KnowledgeSubmissionRequest(
        Long submitUserId,
        String rawLog,
        String techStack,
        String stage,
        String userSolution,
        String relatedFile,
        String relatedCommand,
        KnowledgeSubmissionDraft draft
) {
}
