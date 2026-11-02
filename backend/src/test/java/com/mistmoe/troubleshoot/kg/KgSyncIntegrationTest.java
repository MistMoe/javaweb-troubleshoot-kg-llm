package com.mistmoe.troubleshoot.kg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mistmoe.troubleshoot.errorcase.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.neo4j.harness.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Executes the production Cypher against an isolated real Neo4j engine; never touches a local database. */
class KgSyncIntegrationTest {
    private Neo4j neo4j;
    private HttpServer http;
    private KgService service;
    private ErrorCaseRepository repository;
    private final AtomicInteger caseWrites = new AtomicInteger();
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach void start() throws Exception {
        neo4j = Neo4jBuilders.newInProcessBuilder().withDisabledServer().build();
        repository = mock(ErrorCaseRepository.class);
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/commit", exchange -> {
            var body = mapper.readTree(exchange.getRequestBody());
            Map<String, Object> response;
            try (var tx = neo4j.defaultDatabaseService().beginTx()) {
                List<Object> results = new ArrayList<>();
                for (var statement : body.path("statements")) {
                    String query = statement.path("statement").asText();
                    if (query.contains("MERGE (e:Error")) caseWrites.incrementAndGet();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = mapper.convertValue(statement.path("parameters"), Map.class);
                    // Jackson reads small integer IDs as Integer; Cypher accepts both numeric representations.
                    try (var result = tx.execute(query, params)) {
                        List<Object> data = new ArrayList<>();
                        List<String> columns = result.columns();
                        while (result.hasNext()) {
                            var row = result.next();
                            if (statement.has("resultDataContents")) {
                                List<Object> nodes = new ArrayList<>(), relationships = new ArrayList<>();
                                for (Object value : row.values()) if (value instanceof org.neo4j.graphdb.Path path) {
                                    for (var node : path.nodes()) {
                                        List<String> labels = new ArrayList<>();
                                        node.getLabels().forEach(label -> labels.add(label.name()));
                                        nodes.add(Map.of("id", String.valueOf(node.getId()), "labels", labels,
                                                "properties", node.getAllProperties()));
                                    }
                                    for (var edge : path.relationships()) relationships.add(Map.of(
                                            "id", String.valueOf(edge.getId()), "type", edge.getType().name(),
                                            "startNode", String.valueOf(edge.getStartNodeId()),
                                            "endNode", String.valueOf(edge.getEndNodeId())));
                                }
                                data.add(Map.of("graph", Map.of("nodes", nodes, "relationships", relationships)));
                            } else data.add(Map.of("row", columns.stream().map(row::get).toList()));
                        }
                        results.add(Map.of("data", data));
                    }
                }
                tx.commit();
                response = Map.of("results", results, "errors", List.of());
            } catch (Exception failure) {
                response = Map.of("results", List.of(), "errors", List.of(Map.of("message", failure.getMessage())));
            }
            byte[] bytes = mapper.writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        http.start();
        service = new KgService(repository, "http://127.0.0.1:" + http.getAddress().getPort() + "/commit", "test", "test");
    }
    @AfterEach void stop() {
        if (http != null) http.stop(0);
        if (neo4j != null) neo4j.close();
    }
    private ErrorCase item(long id, String solution, String file) {
        return new ErrorCase(id, "故障" + id, "启动", "Spring Boot", "keyword", "现象", "共享原因", solution,
                file, "command" + id, "风险", null, null, null, null);
    }
    private long count(String query) {
        try (var tx = neo4j.defaultDatabaseService().beginTx(); var result = tx.execute(query)) {
            return ((Number) result.next().values().iterator().next()).longValue();
        }
    }
    @Test void incrementalUpdateAndDeletePreserveOtherCasesSharingEntities() {
        var a = item(1, "共享方案", "a.yml");
        var b = item(2, "共享方案", "b.yml");
        when(repository.list(null, null, null)).thenReturn(List.of(a, b));
        assertThat(service.importFromMysql().importedCases()).isEqualTo(2);
        assertThat(count("MATCH (n:Cause) RETURN count(n)")).isEqualTo(1);
        assertThat(count("MATCH (n:Solution) RETURN count(n)")).isEqualTo(1);
        assertThat(service.graphByErrorId(1L).nodes()).extracting(GraphNode::name).contains("a.yml").doesNotContain("b.yml");
        long edges = count("MATCH ()-[r]->() RETURN count(r)");
        service.importFromMysql();
        assertThat(caseWrites.get()).isEqualTo(2);
        assertThat(count("MATCH ()-[r]->() RETURN count(r)")).isEqualTo(edges);
        when(repository.list(null, null, null)).thenReturn(List.of(item(1, "新方案", "c.yml"), b));
        service.importFromMysql();
        assertThat(caseWrites.get()).isEqualTo(3);
        assertThat(service.graphByErrorId(1L).nodes()).extracting(GraphNode::name).contains("c.yml").doesNotContain("b.yml", "a.yml");
        assertThat(count("MATCH (e:Error {mysqlId: 1})-[:MAY_CAUSED_BY]->()-[r:SOLVED_BY]->(s:Solution {name:'共享方案'}) WHERE r.caseId=1 RETURN count(r)")).isZero();
        assertThat(count("MATCH (:Solution {name:'共享方案'})-[r:CHECK_FILE {caseId:2}]->(:File {name:'b.yml'}) RETURN count(r)")).isEqualTo(1);
        assertThat(count("MATCH (n:File {name:'a.yml'}) RETURN count(n)")).isZero();
        when(repository.list(null, null, null)).thenReturn(List.of(b));
        service.importFromMysql();
        assertThat(count("MATCH (n:Error) RETURN count(n)")).isEqualTo(1);
        assertThat(count("MATCH ()-[r]->() WHERE r.caseId=1 RETURN count(r)")).isZero();
        assertThat(count("MATCH (n:Solution {name:'新方案'}) RETURN count(n)")).isZero();
        when(repository.list(null, null, null)).thenReturn(List.of());
        service.importFromMysql();
        assertThat(count("MATCH (n) RETURN count(n)")).isZero();
    }
    @Test void legacyGraphMigratesAndFailedWriteRollsBackOldRelationships() {
        try (var tx = neo4j.defaultDatabaseService().beginTx()) {
            tx.execute("CREATE (e:Error {mysqlId:1})-[:MAY_CAUSED_BY]->(c:Cause {name:'旧原因'}), (c)-[:SOLVED_BY]->(:Solution {name:'旧方案'})").close();
            tx.commit();
        }
        var a = item(1, "新方案", "a.yml");
        when(repository.list(null, null, null)).thenReturn(List.of(a));
        service.importFromMysql();
        assertThat(count("MATCH ()-[r]->() WHERE r.caseId IS NULL RETURN count(r)")).isZero();
        assertThat(count("MATCH (n:Cause {name:'旧原因'}) RETURN count(n)")).isZero();
        long edges = count("MATCH ()-[r]->() RETURN count(r)");
        var invalid = new ErrorCase(1L, "invalid", null, a.component(), a.keyword(), a.symptom(), a.cause(),
                a.solution(), a.relatedFile(), a.relatedCommand(), a.riskTip(), null, null, null, null);
        when(repository.list(null, null, null)).thenReturn(List.of(invalid));
        assertThatThrownBy(service::importFromMysql).isInstanceOf(IllegalStateException.class);
        assertThat(count("MATCH ()-[r]->() RETURN count(r)")).isEqualTo(edges);
    }
}
