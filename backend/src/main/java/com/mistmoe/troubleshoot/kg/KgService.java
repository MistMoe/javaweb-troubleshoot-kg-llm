package com.mistmoe.troubleshoot.kg;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
        requestFactory.setReadTimeout(30000);
        this.restClient = RestClient.builder()
                .baseUrl(httpUrl)
                .requestFactory(requestFactory)
                .build();
        String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.authorizationHeader = "Basic " + token;
    }

    public KgImportResponse importFromMysql() {
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
        for (ErrorCase item : cases) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("id", item.id());
            params.put("errorName", item.errorName());
            params.put("keyword", item.keyword());
            params.put("stage", item.stage());
            params.put("symptom", item.symptom());
            params.put("cause", item.cause());
            params.put("solution", item.solution());
            statements.add(statement("""
                    MERGE (e:Error {mysqlId: $id})
                    SET e.name = $errorName, e.keyword = $keyword
                    MERGE (stage:Stage {name: $stage})
                    MERGE (symptom:Symptom {name: $symptom})
                    MERGE (cause:Cause {name: $cause})
                    MERGE (solution:Solution {name: $solution})
                    MERGE (e)-[:OCCURS_IN]->(stage)
                    MERGE (e)-[:HAS_SYMPTOM]->(symptom)
                    MERGE (e)-[:MAY_CAUSED_BY]->(cause)
                    MERGE (cause)-[:SOLVED_BY]->(solution)
                    """, params));

            for (String component : splitParts(item.component())) {
                statements.add(statement("""
                        MATCH (e:Error {mysqlId: $id})
                        MERGE (component:Component {name: $name})
                        MERGE (e)-[:RELATED_TO]->(component)
                        """, Map.of("id", item.id(), "name", component)));
            }
            for (String file : splitParts(item.relatedFile())) {
                statements.add(statement("""
                        MATCH (solution:Solution {name: $solution})
                        MERGE (file:File {name: $name})
                        MERGE (solution)-[:CHECK_FILE]->(file)
                        """, Map.of("solution", item.solution(), "name", file)));
            }
            for (String command : splitParts(item.relatedCommand())) {
                statements.add(statement("""
                        MATCH (solution:Solution {name: $solution})
                        MERGE (command:Command {name: $name})
                        MERGE (solution)-[:USE_COMMAND]->(command)
                        """, Map.of("solution", item.solution(), "name", command)));
            }
        }
        runStatements(statements);
        return new KgImportResponse(cases.size());
    }

    public GraphResponse graphByErrorId(Long errorId) {
        JsonNode response = runStatements(List.of(statement("""
                MATCH (e:Error {mysqlId: $errorId})
                OPTIONAL MATCH path=(e)-[*1..3]->(n)
                RETURN path
                """, Map.of("errorId", errorId), List.of("graph"))));
        return parseGraph(response);
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
