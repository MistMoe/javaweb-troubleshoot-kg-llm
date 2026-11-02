package com.mistmoe.troubleshoot.llm;

import com.mistmoe.troubleshoot.analyze.*;
import com.mistmoe.troubleshoot.errorcase.ErrorCase;
import com.mistmoe.troubleshoot.kg.GraphResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class TroubleshootAgentTest {
    private ErrorAnalysisService.Evidence evidence() {
        var item = new ErrorCase(1L, "端口占用", "启动", "Spring Boot", "Port 8080", "启动失败", "进程冲突",
                "修改端口", "application.yml", "netstat", "先确认进程", null, null, null, null);
        return new ErrorAnalysisService.Evidence(new HybridCaseRetriever.Retrieval(
                List.of(new HybridCaseRetriever.Hit(item, 10, 0, 1.2)), "keyword"), item,
                new GraphResponse(List.of(), List.of()), "图谱不可用");
    }
    private LlmProperties config(HttpServer server) {
        var properties = new LlmProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setApiUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
        return properties;
    }
    @Test void realStreamingProtocolRunsTwoToolsThenDeliversTokens() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger calls = new AtomicInteger();
        List<String> requests = Collections.synchronizedList(new ArrayList<>());
        server.createContext("/v1/chat/completions", exchange -> {
            requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int turn = calls.incrementAndGet();
            String delta = turn <= 2 ? """
                    {"tool_calls":[{"index":0,"id":"call_%d","type":"function","function":{"name":"%s","arguments":"{}"}}]}
                    """.formatted(turn, turn == 1 ? "searchCases" : "queryGraph").trim()
                    : "{\"content\":\"建议修改端口\"}";
            String chunk = "data: {\"id\":\"test\",\"object\":\"chat.completion.chunk\",\"choices\":[{\"index\":0,\"delta\":"
                    + delta + ",\"finish_reason\":null}]}\n\n";
            String finish = "data: {\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\""
                    + (turn <= 2 ? "tool_calls" : "stop") + "\"}]}\n\ndata: [DONE]\n\n";
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write(chunk.getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().flush();
            exchange.getResponseBody().write(finish.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.start();
        try {
            List<String> events = Collections.synchronizedList(new ArrayList<>());
            String answer = new TroubleshootAgent(config(server), new ModelCircuitBreaker()).generate(
                    new AnalyzeRequest("Port 8080", "Spring Boot", 3L), evidence(),
                    (event, data) -> events.add(event + ":" + data), () -> true,
                    System.nanoTime() + TimeUnit.SECONDS.toNanos(10));
            assertThat(answer).isEqualTo("建议修改端口");
            assertThat(events).contains("tool:案例检索", "tool:图谱查询", "token:建议修改端口");
            assertThat(calls.get()).isEqualTo(3);
            assertThat(requests.get(2)).contains("tool_call_id", "图谱不可用", "修改端口");
        } finally { server.stop(0); }
    }
    @Test void providerErrorsOpenCircuitAndReturnLocalEvidence() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/v1/chat/completions", exchange -> {
            calls.incrementAndGet(); exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(401, -1); exchange.close();
        });
        server.start();
        try {
            var agent = new TroubleshootAgent(config(server), new ModelCircuitBreaker());
            List<String> events = new ArrayList<>();
            for (int i = 0; i < 4; i++) assertThat(agent.generate(
                    new AnalyzeRequest("Port 8080", "Spring Boot", 3L), evidence(),
                    (event, data) -> events.add(event + ":" + data), () -> true,
                    System.nanoTime() + TimeUnit.SECONDS.toNanos(5))).contains("本地知识库", "修改端口");
            assertThat(calls.get()).isEqualTo(3);
            assertThat(events).anyMatch(e -> e.startsWith("reset:")).anyMatch(e -> e.contains("熔断中"));
        } finally { server.stop(0); }
    }
    @Test void expiredDeadlineFallsBackWithoutContactingProvider() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            List<String> events = new ArrayList<>();
            String answer = new TroubleshootAgent(config(server), new ModelCircuitBreaker()).generate(
                    new AnalyzeRequest("Port 8080", "Spring Boot", 3L), evidence(),
                    (event, data) -> events.add(event), () -> true, System.nanoTime() - 1);
            assertThat(answer).contains("本地知识库");
            assertThat(events).contains("reset");
        } finally { server.stop(0); }
    }
    @Test void toolBudgetAndCancellationPreventFurtherExecution() {
        var tools = new TroubleshootAgent.EvidenceTools(evidence(), (event, data) -> {}, () -> true);
        for (int i = 0; i < 6; i++) tools.searchCases();
        assertThatThrownBy(tools::queryGraph).isInstanceOf(IllegalStateException.class);
        var cancelled = new TroubleshootAgent.EvidenceTools(evidence(), (event, data) -> {}, () -> false);
        assertThatThrownBy(cancelled::searchCases).isInstanceOf(java.util.concurrent.CancellationException.class);
    }
}
