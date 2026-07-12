package com.mistmoe.troubleshoot.errorcase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listReturnsSeededErrorCases() throws Exception {
        mockMvc.perform(get("/api/error/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void listFiltersByKeywordAndComponent() throws Exception {
        mockMvc.perform(get("/api/error/list")
                        .param("keyword", "Port 8080")
                        .param("component", "Spring Boot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].errorName").value("端口被占用"));
    }

    @Test
    void createUpdateAndDeleteErrorCase() throws Exception {
        String createResponse = mockMvc.perform(post("/api/error/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "errorName": "测试故障",
                                  "stage": "接口调用",
                                  "component": "Axios",
                                  "keyword": "demo keyword",
                                  "symptom": "demo symptom",
                                  "cause": "demo cause",
                                  "solution": "demo solution",
                                  "relatedFile": "demo.js",
                                  "relatedCommand": "npm run dev",
                                  "riskTip": "demo risk",
                                  "userId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.errorName").value("测试故障"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        long id = created.get("id").asLong();

        mockMvc.perform(put("/api/error/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "errorName": "测试故障已更新",
                                  "stage": "接口调用",
                                  "component": "Axios",
                                  "keyword": "demo keyword",
                                  "symptom": "demo symptom",
                                  "cause": "updated cause",
                                  "solution": "updated solution",
                                  "relatedFile": "demo.js",
                                  "relatedCommand": "npm run dev",
                                  "riskTip": "updated risk",
                                  "userId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorName").value("测试故障已更新"))
                .andExpect(jsonPath("$.cause").value("updated cause"));

        mockMvc.perform(delete("/api/error/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }
}
