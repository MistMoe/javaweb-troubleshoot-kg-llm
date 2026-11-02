package com.mistmoe.troubleshoot.analyze;

import com.mistmoe.troubleshoot.auth.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/error")
public class ErrorAnalysisController {

    private final ErrorAnalysisService errorAnalysisService;
    private final AnalysisStreamService streams;

    public ErrorAnalysisController(ErrorAnalysisService errorAnalysisService, AnalysisStreamService streams) {
        this.errorAnalysisService = errorAnalysisService;
        this.streams = streams;
    }

    @PostMapping(value = "/analyze/stream", produces = "text/event-stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter stream(
            @RequestBody AnalyzeRequest request, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        return streams.stream(request);
    }

    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request) {
        return errorAnalysisService.analyze(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }
}
