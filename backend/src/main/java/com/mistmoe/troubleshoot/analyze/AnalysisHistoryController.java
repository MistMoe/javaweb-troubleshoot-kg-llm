package com.mistmoe.troubleshoot.analyze;

import java.util.Map;

import com.mistmoe.troubleshoot.auth.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis-history")
public class AnalysisHistoryController {

    private final AnalysisHistoryService service;

    public AnalysisHistoryController(AnalysisHistoryService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean matched
    ) {
        return Map.of("items", service.list(userId, keyword, matched));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        return service.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("分析历史不存在")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!service.delete(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("分析历史不存在"));
        }
        return ResponseEntity.ok(Map.of("deleted", true));
    }
}
