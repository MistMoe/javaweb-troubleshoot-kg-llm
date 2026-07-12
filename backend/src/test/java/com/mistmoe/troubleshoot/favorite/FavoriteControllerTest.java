package com.mistmoe.troubleshoot.favorite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void addListAndRemoveFavoriteCase() throws Exception {
        mockMvc.perform(get("/api/favorites/cases").param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        mockMvc.perform(post("/api/favorites/cases/{caseId}", 1).param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true))
                .andExpect(jsonPath("$.created").value(true));

        mockMvc.perform(post("/api/favorites/cases/{caseId}", 1).param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true))
                .andExpect(jsonPath("$.created").value(false));

        mockMvc.perform(get("/api/favorites/case-ids").param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids", hasItem(1)));

        mockMvc.perform(get("/api/favorites/cases").param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].errorCaseId").value(1));

        mockMvc.perform(delete("/api/favorites/cases/{caseId}", 1).param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false))
                .andExpect(jsonPath("$.deleted").value(true));

        mockMvc.perform(get("/api/favorites/case-ids").param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids", hasSize(0)));
    }
}
