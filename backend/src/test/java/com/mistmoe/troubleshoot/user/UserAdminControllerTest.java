package com.mistmoe.troubleshoot.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        Long qaUserId = jdbcTemplate.query(
                "SELECT id FROM sys_user WHERE username IN ('qauser', 'qauser2')",
                rs -> rs.next() ? rs.getLong("id") : null
        );
        if (qaUserId != null) {
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", qaUserId);
            jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", qaUserId);
        }
    }

    @Test
    void listUsersIncludesRoles() throws Exception {
        mockMvc.perform(get("/api/users/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.items[0].roles").isArray());
    }

    @Test
    void listRolesReturnsSystemRoles() throws Exception {
        mockMvc.perform(get("/api/users/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].roleCode").value("ADMIN"));
    }

    @Test
    void createAndUpdateUserWithRoles() throws Exception {
        String createResponse = mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "qauser",
                                  "password": "123456",
                                  "nickname": "QA User",
                                  "email": "qa@example.com",
                                  "status": 1,
                                  "roleIds": [3]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("qauser"))
                .andExpect(jsonPath("$.roles[0].roleCode").value("USER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        long userId = created.get("id").asLong();

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "qauser2",
                                  "nickname": "QA Maintainer",
                                  "email": "maintainer-qa@example.com",
                                  "status": 1,
                                  "roleIds": [2]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("qauser2"))
                .andExpect(jsonPath("$.roles[0].roleCode").value("MAINTAINER"));
    }

    @Test
    void updateUserStatus() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password, nickname, email, status)
                VALUES (20, 'qauser', '123456', 'QA User', 'qa@example.com', 1)
                """);
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (20, 3)");

        mockMvc.perform(put("/api/users/{id}/status", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0));
    }
}
