package com.mistmoe.troubleshoot.submission;

import java.time.LocalDateTime;

public record KnowledgeSubmission(
        Long id,
        Long submitUserId,
        String submitUsername,
        String submitNickname,
        String rawLog,
        String techStack,
        String stage,
        String userSolution,
        String relatedFile,
        String relatedCommand,
        String draftErrorName,
        String draftKeyword,
        String draftSymptom,
        String draftCause,
        String draftSolution,
        String draftRelatedFile,
        String draftRelatedCommand,
        String draftRiskTip,
        String status,
        Long reviewerId,
        String reviewerUsername,
        String reviewerNickname,
        String reviewComment,
        Long approvedErrorCaseId,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
