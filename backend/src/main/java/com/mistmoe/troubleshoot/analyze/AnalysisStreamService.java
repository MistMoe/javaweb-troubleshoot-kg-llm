package com.mistmoe.troubleshoot.analyze;

import com.mistmoe.troubleshoot.llm.TroubleshootAgent;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

@Service
public class AnalysisStreamService {
    private final ErrorAnalysisService analysis;
    private final TroubleshootAgent agent;
    private final ExecutorService workers = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8), task -> { Thread t = new Thread(task, "analysis-stream"); t.setDaemon(true); return t; });
    public AnalysisStreamService(ErrorAnalysisService analysis, TroubleshootAgent agent) {
        this.analysis = analysis; this.agent = agent;
    }
    public SseEmitter stream(AnalyzeRequest request) {
        ErrorAnalysisService.validate(request);
        SseEmitter emitter = new SseEmitter(65_000L);
        AtomicBoolean active = new AtomicBoolean(true);
        AtomicReference<Future<?>> work = new AtomicReference<>();
        Runnable cancel = () -> { active.set(false); Future<?> task = work.get(); if (task != null) task.cancel(true); };
        emitter.onCompletion(cancel);
        emitter.onTimeout(cancel);
        emitter.onError(error -> cancel.run());
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        try {
            work.set(workers.submit(() -> {
                try {
                    var evidence = analysis.prepare(request, message -> send(emitter, active, "progress", message));
                    send(emitter, active, "retrieval", evidence.retrieval());
                    String advice = agent.generate(request, evidence,
                            (event, data) -> send(emitter, active, event, data),
                            () -> active.get() && !Thread.currentThread().isInterrupted(), deadline);
                    if (!active.get() || Thread.currentThread().isInterrupted()) return;
                    var result = analysis.save(request, evidence, advice);
                    send(emitter, active, "result", result);
                    emitter.complete();
                } catch (CancellationException cancelled) {
                    emitter.complete();
                } catch (Exception failure) {
                    if (active.get()) {
                        try { send(emitter, active, "failure", "分析失败，请检查知识库连接后重试"); }
                        finally { emitter.complete(); }
                    }
                }
            }));
        } catch (RejectedExecutionException busy) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "分析任务繁忙，请稍后重试");
        }
        return emitter;
    }
    private void send(SseEmitter emitter, AtomicBoolean active, String event, Object value) {
        if (!active.get()) throw new CancellationException("Client disconnected");
        try { emitter.send(SseEmitter.event().name(event).data(java.util.Map.of("value", value), org.springframework.http.MediaType.APPLICATION_JSON)); }
        catch (IOException | IllegalStateException failure) {
            active.set(false);
            throw new CancellationException("Client disconnected");
        }
    }
    @PreDestroy
    public void shutdown() { workers.shutdownNow(); }
}
