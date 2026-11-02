package com.mistmoe.troubleshoot.analyze;

import com.mistmoe.troubleshoot.kg.KgService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "llm.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisStreamControllerTest {
    @Autowired MockMvc mvc;
    @MockBean KgService kg;
    @Test void streamsProgressAndFinalFallbackEvenWhenGraphIsDown() throws Exception {
        when(kg.graphByErrorId(1L)).thenThrow(new IllegalStateException("offline"));
        var result = mvc.perform(post("/api/error/analyze/stream").contentType("application/json")
                .content("""
                        {"inputText":"Port 8080 already in use", "techStack":"Spring Boot", "userId":3}
                        """)).andExpect(request().asyncStarted()).andReturn();
        result.getAsyncResult(5000);
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        String wire = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(wire).contains("event:progress", "event:retrieval", "event:result", "本地知识库", "图谱不可用");
        assertThat(wire).contains("data:{\"value\":");
    }
    @Test void rejectsEmptyInputBeforeOpeningStream() throws Exception {
        mvc.perform(post("/api/error/analyze/stream").contentType("application/json")
                .content("{\"inputText\":\" \"}")).andExpect(status().isBadRequest());
    }
    @Test void noMatchStillProducesACompleteResult() throws Exception {
        var result = mvc.perform(post("/api/error/analyze/stream").contentType("application/json")
                .content("{\"inputText\":\"unrecognised-new-error\",\"userId\":3}"))
                .andExpect(request().asyncStarted()).andReturn();
        result.getAsyncResult(5000);
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        assertThat(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .contains("event:result", "\"matched\":false");
        verifyNoInteractions(kg);
    }
}
