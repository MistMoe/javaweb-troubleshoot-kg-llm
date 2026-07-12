package com.mistmoe.troubleshoot.submission;

import java.util.List;
import java.util.Optional;

import com.mistmoe.troubleshoot.errorcase.ErrorCase;
import com.mistmoe.troubleshoot.errorcase.ErrorCaseRepository;
import com.mistmoe.troubleshoot.errorcase.ErrorCaseRequest;
import com.mistmoe.troubleshoot.llm.LlmAdviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeSubmissionService {

    private final KnowledgeSubmissionRepository repository;
    private final ErrorCaseRepository errorCaseRepository;
    private final LlmAdviceService llmAdviceService;

    public KnowledgeSubmissionService(KnowledgeSubmissionRepository repository,
                                      ErrorCaseRepository errorCaseRepository,
                                      LlmAdviceService llmAdviceService) {
        this.repository = repository;
        this.errorCaseRepository = errorCaseRepository;
        this.llmAdviceService = llmAdviceService;
    }

    public KnowledgeSubmissionDraft draft(KnowledgeSubmissionRequest request) {
        validateRawSubmission(request);
        return llmAdviceService.generateCaseDraft(request);
    }

    public KnowledgeSubmission create(KnowledgeSubmissionRequest request) {
        validateRawSubmission(request);
        KnowledgeSubmissionDraft draft = normalizeDraft(request.draft());
        validateDraft(draft);
        return repository.create(request, draft);
    }

    public List<KnowledgeSubmission> list(String status, Long submitUserId) {
        return repository.list(status, submitUserId);
    }

    public Optional<KnowledgeSubmission> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Optional<KnowledgeSubmission> approve(Long id, KnowledgeSubmissionReviewRequest request) {
        validateReviewer(request);
        KnowledgeSubmission submission = repository.findById(id).orElse(null);
        if (submission == null || !KnowledgeSubmissionRepository.STATUS_PENDING.equals(submission.status())) {
            return Optional.empty();
        }
        KnowledgeSubmissionDraft draft = request.draft() == null
                ? draftFromSubmission(submission)
                : normalizeDraft(request.draft());
        validateDraft(draft);
        ErrorCase createdCase = errorCaseRepository.create(new ErrorCaseRequest(
                draft.errorName(),
                firstText(submission.stage(), submission.techStack()),
                firstText(submission.techStack(), "用户投稿"),
                draft.keyword(),
                draft.symptom(),
                draft.cause(),
                draft.solution(),
                firstText(draft.relatedFile(), submission.relatedFile()),
                firstText(draft.relatedCommand(), submission.relatedCommand()),
                draft.riskTip(),
                request.reviewerId()
        ));
        return repository.approve(id, request.reviewerId(), request.reviewComment(), draft, createdCase.id());
    }

    public Optional<KnowledgeSubmission> reject(Long id, KnowledgeSubmissionReviewRequest request) {
        validateReviewer(request);
        if (!StringUtils.hasText(request.reviewComment())) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }
        return repository.reject(id, request.reviewerId(), request.reviewComment());
    }

    private void validateRawSubmission(KnowledgeSubmissionRequest request) {
        if (request == null || request.submitUserId() == null) {
            throw new IllegalArgumentException("提交用户不能为空");
        }
        if (!StringUtils.hasText(request.rawLog())) {
            throw new IllegalArgumentException("报错日志不能为空");
        }
        if (!StringUtils.hasText(request.techStack())) {
            throw new IllegalArgumentException("技术栈不能为空");
        }
        if (!StringUtils.hasText(request.userSolution())) {
            throw new IllegalArgumentException("解决过程不能为空");
        }
    }

    private void validateReviewer(KnowledgeSubmissionReviewRequest request) {
        if (request == null || request.reviewerId() == null) {
            throw new IllegalArgumentException("审核人不能为空");
        }
        if (!repository.reviewerCanApprove(request.reviewerId())) {
            throw new IllegalArgumentException("只有 ADMIN 或 MAINTAINER 可以审核投稿");
        }
    }

    private void validateDraft(KnowledgeSubmissionDraft draft) {
        if (!StringUtils.hasText(draft.errorName())) {
            throw new IllegalArgumentException("候选故障名称不能为空");
        }
        if (!StringUtils.hasText(draft.keyword())) {
            throw new IllegalArgumentException("候选关键词不能为空");
        }
        if (!StringUtils.hasText(draft.solution())) {
            throw new IllegalArgumentException("候选解决方案不能为空");
        }
    }

    private KnowledgeSubmissionDraft normalizeDraft(KnowledgeSubmissionDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("AI 候选案例不能为空");
        }
        return new KnowledgeSubmissionDraft(
                trim(draft.errorName()),
                trim(draft.keyword()),
                trim(draft.symptom()),
                trim(draft.cause()),
                trim(draft.solution()),
                trim(draft.relatedFile()),
                trim(draft.relatedCommand()),
                trim(draft.riskTip())
        );
    }

    private KnowledgeSubmissionDraft draftFromSubmission(KnowledgeSubmission submission) {
        return new KnowledgeSubmissionDraft(
                submission.draftErrorName(),
                submission.draftKeyword(),
                submission.draftSymptom(),
                submission.draftCause(),
                submission.draftSolution(),
                submission.draftRelatedFile(),
                submission.draftRelatedCommand(),
                submission.draftRiskTip()
        );
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
