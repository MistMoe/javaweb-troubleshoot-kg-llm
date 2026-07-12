package com.mistmoe.troubleshoot.analyze;

import java.util.List;

import com.mistmoe.troubleshoot.kg.GraphLink;
import com.mistmoe.troubleshoot.kg.GraphNode;
import com.mistmoe.troubleshoot.kg.GraphResponse;
import com.mistmoe.troubleshoot.kg.KgService;
import com.mistmoe.troubleshoot.llm.LlmAdviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private KgService kgService;

    @MockBean
    private LlmAdviceService llmAdviceService;

    @Test
    void analyzeMatchesErrorCaseAndStoresHistory() throws Exception {
        when(kgService.graphByErrorId(1L)).thenReturn(new GraphResponse(
                List.of(new GraphNode("1", "Port 8080 already in use", "Error")),
                List.of(new GraphLink("1", "2", "OCCURS_IN"))
        ));
        when(llmAdviceService.generateAdvice(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("LLM structured advice from graph evidence");

        mockMvc.perform(post("/api/error/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputText": "Web server failed to start. Port 8080 already in use.",
                                  "techStack": "Spring Boot",
                                  "userId": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.errorId").value(1))
                .andExpect(jsonPath("$.component").value("Spring Boot"))
                .andExpect(jsonPath("$.causes[0]").exists())
                .andExpect(jsonPath("$.solutions[0]").exists())
                .andExpect(jsonPath("$.files[0]").value("application.yml"))
                .andExpect(jsonPath("$.commands[0]").value("netstat -ano | findstr 8080"))
                .andExpect(jsonPath("$.graph.nodes[0].category").value("Error"))
                .andExpect(jsonPath("$.aiAdvice").value("LLM structured advice from graph evidence"));

        Integer historyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analysis_history WHERE user_id = 3 AND matched_error_id = 1",
                Integer.class
        );
        org.assertj.core.api.Assertions.assertThat(historyCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void analyzeReturnsHelpfulMessageWhenNoCaseMatches() throws Exception {
        mockMvc.perform(post("/api/error/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputText": "some brand new unknown stack trace",
                                  "techStack": "Spring Boot",
                                  "userId": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(false))
                .andExpect(jsonPath("$.graph.nodes").isArray())
                .andExpect(jsonPath("$.message").exists());

        Integer historyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analysis_history WHERE user_id = 3 AND matched_error_id IS NULL",
                Integer.class
        );
        org.assertj.core.api.Assertions.assertThat(historyCount).isGreaterThanOrEqualTo(1);
    }
}
