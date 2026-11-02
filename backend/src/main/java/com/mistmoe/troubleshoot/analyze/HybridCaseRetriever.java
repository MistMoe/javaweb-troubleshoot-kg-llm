package com.mistmoe.troubleshoot.analyze;

import com.mistmoe.troubleshoot.errorcase.ErrorCase;
import com.mistmoe.troubleshoot.errorcase.ErrorCaseRepository;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;

@Service
public class HybridCaseRetriever {
    public record Hit(ErrorCase errorCase, int keywordScore, double similarity, double score) {}
    public record Retrieval(List<Hit> hits, String mode) {}
    private final ErrorCaseRepository repository;
    private final EmbeddingModel embeddings;
    private final double threshold;
    private final Map<Long, CachedVector> cache = new HashMap<>();
    private record CachedVector(String text, float[] vector) {}

    @org.springframework.beans.factory.annotation.Autowired
    public HybridCaseRetriever(ErrorCaseRepository repository,
            @Value("${rag.embedding.base-url:${EMBEDDING_BASE_URL:}}") String baseUrl,
            @Value("${rag.embedding.api-key:${EMBEDDING_API_KEY:}}") String apiKey,
            @Value("${rag.embedding.model:${EMBEDDING_MODEL:BAAI/bge-m3}}") String model,
            @Value("${rag.embedding.min-score:0.65}") double threshold) {
        this(repository, baseUrl.isBlank() ? null : OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl).apiKey(apiKey.isBlank() ? "local" : apiKey).modelName(model)
                .timeout(Duration.ofSeconds(5)).maxRetries(0).build(), threshold);
    }

    HybridCaseRetriever(ErrorCaseRepository repository, EmbeddingModel embeddings, double threshold) {
        this.repository = repository;
        this.embeddings = embeddings;
        this.threshold = threshold;
    }

    // shortcut: in-memory vectors suit the current small corpus; use a persistent index as it grows.
    public synchronized Retrieval retrieve(String query, String techStack) {
        List<ErrorCase> cases = repository.list(null, null, null);
        Map<Long, Double> similarities = new HashMap<>();
        String mode = "keyword (embedding not configured)";
        if (embeddings != null && !cases.isEmpty()) {
            try {
                long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                cache.keySet().retainAll(cases.stream().map(ErrorCase::id).toList());
                List<ErrorCase> changed = cases.stream().filter(c -> !cache.containsKey(c.id())
                        || !cache.get(c.id()).text().equals(document(c))).toList();
                if (!changed.isEmpty()) {
                    var vectors = embeddings.embedAll(changed.stream()
                            .map(c -> dev.langchain4j.data.segment.TextSegment.from(document(c))).toList()).content();
                    if (vectors.size() != changed.size()) throw new IllegalStateException("Embedding count mismatch");
                    for (int i = 0; i < changed.size(); i++) {
                        cache.put(changed.get(i).id(), new CachedVector(document(changed.get(i)), vectors.get(i).vector()));
                    }
                }
                if (System.nanoTime() > deadline) throw new IllegalStateException("Embedding budget exceeded");
                float[] vector = embeddings.embed(query).content().vector();
                for (ErrorCase item : cases) similarities.put(item.id(), cosine(vector, cache.get(item.id()).vector()));
                mode = "hybrid";
            } catch (RuntimeException failure) {
                cache.clear();
                similarities.clear();
                mode = "keyword (embedding unavailable)";
            }
        }
        List<Hit> hits = new ArrayList<>();
        for (ErrorCase item : cases) {
            int keyword = keywordScore(item, query, techStack);
            double similarity = similarities.getOrDefault(item.id(), 0.0);
            if (keyword == 0 && similarity < threshold) continue;
            double score = (keyword > 0 ? 1 + Math.min(keyword, 50) / 50.0 : 0)
                    + (similarity >= threshold ? similarity : 0);
            hits.add(new Hit(item, keyword, similarity, score));
        }
        hits.sort(Comparator.comparingDouble(Hit::score).reversed().thenComparing(h -> h.errorCase().id()));
        return new Retrieval(hits.stream().limit(5).toList(), mode);
    }

    static int keywordScore(ErrorCase item, String query, String techStack) {
        String input = normalize(query);
        int score = 0;
        for (String keyword : normalize(item.keyword()).split("[,;，；/]+")) {
            if (!keyword.isBlank() && input.contains(keyword.trim())) score += 10;
        }
        if (!normalize(item.errorName()).isBlank() && input.contains(normalize(item.errorName()))) score += 6;
        if (score > 0 && !normalize(techStack).isBlank() && normalize(item.component()).contains(normalize(techStack))) score += 3;
        return score;
    }

    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static String document(ErrorCase c) {
        return String.join("\n", java.util.stream.Stream.of(c.errorName(), c.component(), c.keyword(), c.symptom(), c.cause(), c.solution())
                .map(v -> v == null ? "" : v).toList());
    }
    static double cosine(float[] a, float[] b) {
        if (a.length != b.length || a.length == 0) throw new IllegalArgumentException("Embedding dimensions mismatch");
        double dot = 0, aa = 0, bb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i]*b[i]; aa += a[i]*a[i]; bb += b[i]*b[i]; }
        if (aa == 0 || bb == 0 || !Double.isFinite(dot)) throw new IllegalArgumentException("Invalid embedding");
        return dot / Math.sqrt(aa * bb);
    }
}
