package com.mistmoe.troubleshoot.assistant;

import com.mistmoe.troubleshoot.auth.ErrorResponse;
import com.mistmoe.troubleshoot.llm.LlmAdviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final LlmAdviceService llmAdviceService;

    public AssistantController(LlmAdviceService llmAdviceService) {
        this.llmAdviceService = llmAdviceService;
    }

    @PostMapping("/chat")
    public AssistantChatResponse chat(@RequestBody AssistantChatRequest request) {
        if (request == null || request.message() == null || request.message().trim().isEmpty()) {
            throw new IllegalArgumentException("问题内容不能为空");
        }
        return new AssistantChatResponse(llmAdviceService.generateAssistantReply(request.message().trim()), "llm");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }
}
