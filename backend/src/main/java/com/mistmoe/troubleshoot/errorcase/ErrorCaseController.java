package com.mistmoe.troubleshoot.errorcase;

import java.util.Map;

import com.mistmoe.troubleshoot.auth.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/error")
public class ErrorCaseController {

    private final ErrorCaseService errorCaseService;

    public ErrorCaseController(ErrorCaseService errorCaseService) {
        this.errorCaseService = errorCaseService;
    }

    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String component,
            @RequestParam(required = false) String stage
    ) {
        return Map.of("items", errorCaseService.list(keyword, component, stage));
    }

    @PostMapping("/create")
    public ResponseEntity<ErrorCase> create(@RequestBody ErrorCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(errorCaseService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ErrorCaseRequest request) {
        return errorCaseService.update(id, request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("故障案例不存在")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!errorCaseService.delete(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("故障案例不存在"));
        }
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }
}
