package com.mistmoe.troubleshoot.submission;

import java.util.Map;

import com.mistmoe.troubleshoot.auth.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions")
public class KnowledgeSubmissionController {

    private final KnowledgeSubmissionService service;

    public KnowledgeSubmissionController(KnowledgeSubmissionService service) {
        this.service = service;
    }

    @PostMapping("/draft")
    public Map<String, Object> draft(@RequestBody KnowledgeSubmissionRequest request) {
        return Map.of("draft", service.draft(request));
    }

    @PostMapping("/create")
    public ResponseEntity<KnowledgeSubmission> create(@RequestBody KnowledgeSubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long submitUserId
    ) {
        return Map.of("items", service.list(status, submitUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        return service.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("投稿不存在")));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody KnowledgeSubmissionReviewRequest request) {
        return service.approve(id, request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("投稿不存在或已审核")));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody KnowledgeSubmissionReviewRequest request) {
        return service.reject(id, request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("投稿不存在或已审核")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }
}
