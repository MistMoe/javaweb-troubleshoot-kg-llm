package com.mistmoe.troubleshoot.assistant;

import com.mistmoe.troubleshoot.llm.LlmAdviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmAdviceService llmAdviceService;

    @Test
    void chatReturnsAssistantReplyFromLlmService() throws Exception {
        when(llmAdviceService.generateAssistantReply(eq("怎么分析 Spring Boot 启动失败？")))
                .thenReturn("先检查端口、配置文件和依赖冲突。");

        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "怎么分析 Spring Boot 启动失败？"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("先检查端口、配置文件和依赖冲突。"))
                .andExpect(jsonPath("$.source").value("llm"));
    }

    @Test
    void chatRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
