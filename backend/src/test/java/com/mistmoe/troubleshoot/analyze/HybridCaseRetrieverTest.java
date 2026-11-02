package com.mistmoe.troubleshoot.analyze;

import com.mistmoe.troubleshoot.errorcase.*;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class HybridCaseRetrieverTest {
    static ErrorCase item(long id, String keyword, String solution) {
        return new ErrorCase(id, "端口占用", "启动", "Spring Boot", keyword, "端口被占用", "进程冲突", solution,
                "application.yml", "netstat", "确认进程用途", null, null, null, null);
    }
    @Test void semanticRecallDeduplicatesAndReusesUnchangedVectors() {
        var repository = mock(ErrorCaseRepository.class);
        var embeddings = mock(EmbeddingModel.class);
        when(repository.list(null, null, null)).thenReturn(List.of(item(1, "Port 8080", "修改端口")));
        when(embeddings.embedAll(anyList())).thenReturn(Response.from(List.of(Embedding.from(new float[]{1, 0}))));
        when(embeddings.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[]{1, 0})));
        var retriever = new HybridCaseRetriever(repository, embeddings, 0.65);
        var semantic = retriever.retrieve("应用启动时监听地址已经被别的程序占着", "Spring Boot");
        assertThat(semantic.mode()).isEqualTo("hybrid");
        assertThat(semantic.hits()).hasSize(1);
        assertThat(semantic.hits().get(0).keywordScore()).isZero();
        assertThat(retriever.retrieve("Port 8080", "Spring Boot").hits()).hasSize(1);
        verify(embeddings, times(1)).embedAll(anyList());
        when(repository.list(null, null, null)).thenReturn(List.of(item(1, "Port 8080", "停止占用进程")));
        retriever.retrieve("Port 8080", "Spring Boot");
        verify(embeddings, times(2)).embedAll(anyList());
    }
    @Test void embeddingFailureKeepsKeywordsAndRejectsUnrelatedInput() {
        var repository = mock(ErrorCaseRepository.class);
        var embeddings = mock(EmbeddingModel.class);
        when(repository.list(null, null, null)).thenReturn(List.of(item(1, "Port 8080", "修改端口")));
        when(embeddings.embedAll(anyList())).thenThrow(new IllegalStateException("offline"));
        var retriever = new HybridCaseRetriever(repository, embeddings, 0.65);
        assertThat(retriever.retrieve("Port 8080", "Spring Boot").hits()).hasSize(1);
        assertThat(retriever.retrieve("unknown", "Spring Boot").hits()).isEmpty();
        assertThat(retriever.retrieve("unknown", "Spring Boot").mode()).contains("unavailable");
    }
    @Test void mismatchedEmbeddingDimensionsFallBackRatherThanUseBadScores() {
        var repository = mock(ErrorCaseRepository.class);
        var embeddings = mock(EmbeddingModel.class);
        when(repository.list(null, null, null)).thenReturn(List.of(item(1, "Port 8080", "修改端口")));
        when(embeddings.embedAll(anyList())).thenReturn(Response.from(List.of(Embedding.from(new float[]{1, 0}))));
        when(embeddings.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[]{1})));
        var result = new HybridCaseRetriever(repository, embeddings, 0.65).retrieve("unknown", "Spring Boot");
        assertThat(result.hits()).isEmpty();
        assertThat(result.mode()).contains("unavailable");
    }
}
