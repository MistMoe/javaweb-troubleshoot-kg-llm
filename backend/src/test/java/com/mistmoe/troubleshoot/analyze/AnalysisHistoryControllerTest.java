package com.mistmoe.troubleshoot.analyze;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM analysis_history");
        jdbcTemplate.update("""
                INSERT INTO analysis_history (
                    id, user_id, input_text, tech_stack, matched_error_id,
                    matched_error_name, graph_result, ai_advice, create_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                100L,
                3L,
                "Web server failed to start. Port 8080 already in use.",
                "Spring Boot",
                1L,
                "Port 8080 already in use",
                "{\"nodes\":[{\"id\":\"1\",\"name\":\"Port 8080\",\"category\":\"Error\"}],\"links\":[]}",
                "Check the occupied port and update application.yml.",
                "2026-07-08 10:20:00"
        );
        jdbcTemplate.update("""
                INSERT INTO analysis_history (
                    id, user_id, input_text, tech_stack, matched_error_id,
                    matched_error_name, graph_result, ai_advice, create_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                101L,
                1L,
                "Cannot resolve dependency in Maven build.",
                "Maven",
                2L,
                "Maven dependency cannot resolve",
                "{\"nodes\":[],\"links\":[]}",
                "Check pom.xml, settings.xml, and repository mirror.",
                "2026-07-08 11:30:00"
        );
    }

    @Test
    void listReturnsAllHistoryForAdminOrMaintainer() throws Exception {
        mockMvc.perform(get("/api/analysis-history/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(101))
                .andExpect(jsonPath("$.items[0].matched").value(true))
                .andExpect(jsonPath("$.items[0].graphNodeCount").value(0))
                .andExpect(jsonPath("$.items[1].graphNodeCount").value(1));
    }

    @Test
    void listCanFilterByUserIdKeywordAndMatchedState() throws Exception {
        mockMvc.perform(get("/api/analysis-history/list")
                        .param("userId", "3")
                        .param("keyword", "Port")
                        .param("matched", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].userId").value(3))
                .andExpect(jsonPath("$.items[0].techStack").value("Spring Boot"));
    }

    @Test
    void detailReturnsGraphAndAdvice() throws Exception {
        mockMvc.perform(get("/api/analysis-history/{id}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.graph.nodes", hasSize(1)))
                .andExpect(jsonPath("$.graph.links", hasSize(0)))
                .andExpect(jsonPath("$.aiAdvice", is("Check the occupied port and update application.yml.")));
    }

    @Test
    void deleteRemovesHistory() throws Exception {
        mockMvc.perform(delete("/api/analysis-history/{id}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        mockMvc.perform(get("/api/analysis-history/{id}", 100L))
                .andExpect(status().isNotFound());
    }
}
