package com.mistmoe.troubleshoot.kg;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KgControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KgService kgService;

    @Test
    void graphReturnsNodesAndLinksForEcharts() throws Exception {
        when(kgService.graphByErrorId(1L)).thenReturn(new GraphResponse(
                List.of(new GraphNode("error-1", "端口被占用", "Error")),
                List.of(new GraphLink("error-1", "stage-1", "OCCURS_IN"))
        ));

        mockMvc.perform(get("/api/kg/graph/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].category").value("Error"))
                .andExpect(jsonPath("$.links[0].label").value("OCCURS_IN"));
    }

    @Test
    void importReturnsImportedCaseCount() throws Exception {
        when(kgService.importFromMysql()).thenReturn(new KgImportResponse(12));

        mockMvc.perform(post("/api/kg/import"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCases").value(12));
    }
}
