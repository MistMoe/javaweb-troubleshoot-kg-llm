package com.mistmoe.troubleshoot.kg;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.fasterxml.jackson.databind.JsonNode;
import com.mistmoe.troubleshoot.errorcase.ErrorCase;
import com.mistmoe.troubleshoot.errorcase.ErrorCaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class KgService {

    private static final Pattern PART_SPLITTER = Pattern.compile("[,;，；/]+");

    private final RestClient restClient;
    private final ErrorCaseRepository errorCaseRepository;
    private final String authorizationHeader;

    public KgService(
            ErrorCaseRepository errorCaseRepository,
            @Value("${neo4j.http-url}") String httpUrl,
            @Value("${neo4j.username}") String username,
            @Value("${neo4j.password}") String password
    ) {
        this.errorCaseRepository = errorCaseRepository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .baseUrl(httpUrl)
                .requestFactory(requestFactory)
                .build();
        String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.authorizationHeader = "Basic " + token;
    }

    public synchronized KgImportResponse importFromMysql() {
        List<ErrorCase> cases = errorCaseRepository.list(null, null, null);
        runStatements(List.of(
                statement("CREATE CONSTRAINT error_mysql_id IF NOT EXISTS FOR (n:Error) REQUIRE n.mysqlId IS UNIQUE", Map.of()),
                statement("CREATE CONSTRAINT stage_name IF NOT EXISTS FOR (n:Stage) REQUIRE n.name IS UNIQUE", Map.of()),
                statement("CREATE CONSTRAINT component_name IF NOT EXISTS FOR (n:Component) REQUIRE n.name IS UNIQUE", Map.of()),
                statement("CREATE CONSTRAINT symptom_name IF NOT EXISTS FOR (n:Symptom) REQUIRE n.name IS UNIQUE", Map.of()),
                statement("CREATE CONSTRAINT cause_name IF NOT EXISTS FOR (n:Cause) REQUIRE n.name IS UNIQUE", Map.of()),
                statement("CREATE CONSTRAINT solution_name IF NOT EXISTS FOR (n:Solution) REQUIRE n.name IS UNIQUE", Map.of()),
                statement("CREATE CONSTRAINT file_name IF NOT EXISTS FOR (n:File) REQUIRE n.name IS UNIQUE", Map.of()),
                statement("CREATE CONSTRAINT command_name IF NOT EXISTS FOR (n:Command) REQUIRE n.name IS UNIQUE", Map.of())
        ));

        List<Map<String, Object>> statements = new ArrayList<>();
        JsonNode existing = runStatements(List.of(statement(
                "MATCH (e:Error) WHERE e.mysqlId IS NOT NULL RETURN e.mysqlId, e.fingerprint", Map.of())));
        Map<Long, String> fingerprints = new LinkedHashMap<>();
        for (JsonNode row : existing.path("results").path(0).path("data")) {
            fingerprints.put(row.path("row").path(0).asLong(), row.path("row").path(1).asText(""));
        }
        List<Long> ids = cases.stream().map(ErrorCase::id).toList();
        for (Long id : fingerprints.keySet()) {
            if (!ids.contains(id)) {
                statements.add(statement("""
                        MATCH (e:Error {mysqlId: $id}) OPTIONAL MATCH p=(e)-[*1..3]->()
                        UNWIND relationships(p) AS r
                        WITH DISTINCT r WHERE r.caseId IS NULL OR r.caseId = $id DELETE r
                        """, Map.of("id", id)));
                statements.add(statement("MATCH ()-[r]->() WHERE r.caseId = $id DELETE r", Map.of("id", id)));
                statements.add(statement("MATCH (e:Error {mysqlId: $id}) DETACH DELETE e", Map.of("id", id)));
            }
        }
        for (ErrorCase item : cases) {
            String fingerprint = fingerprint(item);
            if (fingerprint.equals(fingerprints.get(item.id()))) continue;
            // Remove legacy paths and this case's owned edges, without deleting shared entities.
            statements.add(statement("""
                    MATCH (e:Error {mysqlId: $id})
                    OPTIONAL MATCH p=(e)-[*1..3]->()
                    UNWIND relationships(p) AS r
                    WITH DISTINCT r WHERE r.caseId IS NULL OR r.caseId = $id DELETE r
                    """, Map.of("id", item.id())));
            statements.add(statement("MATCH ()-[r]->() WHERE r.caseId = $id DELETE r", Map.of("id", item.id())));
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("id", item.id());
            params.put("errorName", item.errorName());
            params.put("keyword", item.keyword());
            params.put("stage", item.stage());
            params.put("symptom", item.symptom());
            params.put("cause", item.cause());
            params.put("solution", item.solution());
            params.put("fingerprint", fingerprint);
            statements.add(statement("""
                    MERGE (e:Error {mysqlId: $id})
                    SET e.name = $errorName, e.keyword = $keyword, e.fingerprint = $fingerprint
                    MERGE (stage:Stage {name: $stage})
                    MERGE (symptom:Symptom {name: $symptom})
                    MERGE (cause:Cause {name: $cause})
                    MERGE (solution:Solution {name: $solution})
                    MERGE (e)-[:OCCURS_IN {caseId: $id}]->(stage)
                    MERGE (e)-[:HAS_SYMPTOM {caseId: $id}]->(symptom)
                    MERGE (e)-[:MAY_CAUSED_BY {caseId: $id}]->(cause)
                    MERGE (cause)-[:SOLVED_BY {caseId: $id}]->(solution)
                    """, params));

            for (String component : splitParts(item.component())) {
                statements.add(statement("""
                        MATCH (e:Error {mysqlId: $id})
                        MERGE (component:Component {name: $name})
                        MERGE (e)-[:RELATED_TO {caseId: $id}]->(component)
                        """, Map.of("id", item.id(), "name", component)));
            }
            for (String file : splitParts(item.relatedFile())) {
                statements.add(statement("""
                        MATCH (solution:Solution {name: $solution})
                        MERGE (file:File {name: $name})
                        MERGE (solution)-[:CHECK_FILE {caseId: $id}]->(file)
                        """, Map.of("solution", item.solution(), "name", file, "id", item.id())));
            }
            for (String command : splitParts(item.relatedCommand())) {
                statements.add(statement("""
                        MATCH (solution:Solution {name: $solution})
                        MERGE (command:Command {name: $name})
                        MERGE (solution)-[:USE_COMMAND {caseId: $id}]->(command)
                        """, Map.of("solution", item.solution(), "name", command, "id", item.id())));
            }
        }
        if (!statements.isEmpty()) {
            statements.add(statement("""
                    MATCH (n) WHERE any(label IN labels(n) WHERE label IN
                    ['Stage','Component','Symptom','Cause','Solution','File','Command'])
                    AND NOT (n)--() DELETE n
                    """, Map.of()));
            runStatements(statements);
        }
        return new KgImportResponse(cases.size());
    }

    public GraphResponse graphByErrorId(Long errorId) {
        JsonNode response = runStatements(List.of(statement("""
                MATCH (e:Error {mysqlId: $errorId})
                OPTIONAL MATCH path=(e)-[*1..3]->(n)
                WHERE all(r IN relationships(path) WHERE r.caseId = $errorId OR r.caseId IS NULL)
                RETURN path
                LIMIT 200
                """, Map.of("errorId", errorId), List.of("graph"))));
        return parseGraph(response);
    }

    static String fingerprint(ErrorCase item) {
        try {
            String text = java.util.stream.Stream.of(item.errorName(), item.stage(),
                    item.component(), item.keyword(), item.symptom(), item.cause(), item.solution(),
                    item.relatedFile(), item.relatedCommand(), item.riskTip())
                    .map(v -> v == null ? "-1:" : v.length() + ":" + v).collect(java.util.stream.Collectors.joining());
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public GraphResponse search(String keyword) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        if (safeKeyword.isEmpty()) {
            return new GraphResponse(List.of(), List.of());
        }
        JsonNode response = runStatements(List.of(statement("""
                MATCH (n)
                WHERE any(label IN labels(n) WHERE label IN ['Error','Stage','Component','Symptom','Cause','Solution','File','Command'])
                  AND toLower(coalesce(n.name, '')) CONTAINS toLower($keyword)
                RETURN n
                LIMIT 30
                """, Map.of("keyword", safeKeyword), List.of("graph"))));
        return parseGraph(response);
    }

    private JsonNode runStatements(List<Map<String, Object>> statements) {
        JsonNode response = restClient.post()
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .body(Map.of("statements", statements))
                .retrieve()
                .body(JsonNode.class);

        JsonNode errors = response == null ? null : response.get("errors");
        if (errors != null && errors.isArray() && !errors.isEmpty()) {
            throw new IllegalStateException(errors.toString());
        }
        return response;
    }

    private Map<String, Object> statement(String statement, Map<String, Object> parameters) {
        return statement(statement, parameters, List.of());
    }

    private Map<String, Object> statement(String statement, Map<String, Object> parameters, List<String> resultDataContents) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("statement", statement);
        item.put("parameters", parameters);
        if (!resultDataContents.isEmpty()) {
            item.put("resultDataContents", resultDataContents);
        }
        return item;
    }

    private GraphResponse parseGraph(JsonNode response) {
        Map<String, GraphNode> nodes = new LinkedHashMap<>();
        Map<String, GraphLink> links = new LinkedHashMap<>();
        JsonNode results = response == null ? null : response.get("results");
        if (results == null || !results.isArray()) {
            return new GraphResponse(List.of(), List.of());
        }

        for (JsonNode result : results) {
            for (JsonNode data : result.path("data")) {
                JsonNode graph = data.path("graph");
                for (JsonNode node : graph.path("nodes")) {
                    String id = node.path("id").asText();
                    String category = node.path("labels").isArray() && !node.path("labels").isEmpty()
                            ? node.path("labels").get(0).asText()
                            : "Node";
                    String name = node.path("properties").path("name").asText("");
                    nodes.put(id, new GraphNode(id, name, category));
                }
                for (JsonNode relationship : graph.path("relationships")) {
                    String id = relationship.path("id").asText();
                    String source = relationship.path("startNode").asText();
                    String target = relationship.path("endNode").asText();
                    String type = relationship.path("type").asText();
                    links.put(id, new GraphLink(source, target, type));
                }
            }
        }
        return new GraphResponse(new ArrayList<>(nodes.values()), new ArrayList<>(links.values()));
    }

    private List<String> splitParts(String value) {
        if (value == null || value.trim().isEmpty()) {
            return List.of();
        }
        List<String> parts = new ArrayList<>();
        for (String part : PART_SPLITTER.split(value)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts;
    }
}
