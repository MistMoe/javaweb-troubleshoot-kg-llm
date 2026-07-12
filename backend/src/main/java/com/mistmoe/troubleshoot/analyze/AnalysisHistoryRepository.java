package com.mistmoe.troubleshoot.analyze;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mistmoe.troubleshoot.kg.GraphResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AnalysisHistoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AnalysisHistoryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(AnalyzeRequest request, Long matchedErrorId, String matchedErrorName,
                     GraphResponse graph, String advice) {
        String graphJson = toJson(graph);
        jdbcTemplate.update("""
                INSERT INTO analysis_history (
                    user_id, input_text, tech_stack, matched_error_id,
                    matched_error_name, graph_result, ai_advice
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                request.userId(),
                request.inputText(),
                request.techStack(),
                matchedErrorId,
                matchedErrorName,
                graphJson,
                advice
        );
    }

    public List<AnalysisHistorySummary> list(Long userId, String keyword, Boolean matched) {
        StringBuilder sql = new StringBuilder("""
                SELECT h.id, h.user_id, u.username, u.nickname, h.input_text, h.tech_stack,
                       h.matched_error_id, h.matched_error_name, h.graph_result, h.ai_advice,
                       h.create_time
                FROM analysis_history h
                LEFT JOIN sys_user u ON u.id = h.user_id
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();

        if (userId != null) {
            sql.append(" AND h.user_id = ? ");
            args.add(userId);
        }
        if (hasText(keyword)) {
            sql.append("""
                    AND (
                        h.input_text LIKE ?
                        OR h.tech_stack LIKE ?
                        OR h.matched_error_name LIKE ?
                        OR h.ai_advice LIKE ?
                    )
                    """);
            String pattern = "%" + keyword.trim() + "%";
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (matched != null) {
            sql.append(matched ? " AND h.matched_error_id IS NOT NULL " : " AND h.matched_error_id IS NULL ");
        }

        sql.append(" ORDER BY h.create_time DESC, h.id DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapSummary(rs), args.toArray());
    }

    public Optional<AnalysisHistoryDetail> findById(Long id) {
        String sql = """
                SELECT h.id, h.user_id, u.username, u.nickname, h.input_text, h.tech_stack,
                       h.matched_error_id, h.matched_error_name, h.graph_result, h.ai_advice,
                       h.create_time
                FROM analysis_history h
                LEFT JOIN sys_user u ON u.id = h.user_id
                WHERE h.id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapDetail(rs), id).stream().findFirst();
    }

    public boolean delete(Long id) {
        return jdbcTemplate.update("DELETE FROM analysis_history WHERE id = ?", id) > 0;
    }

    private AnalysisHistorySummary mapSummary(ResultSet rs) throws SQLException {
        GraphResponse graph = fromJson(rs.getString("graph_result"));
        return new AnalysisHistorySummary(
                rs.getLong("id"),
                getNullableLong(rs, "user_id"),
                rs.getString("username"),
                rs.getString("nickname"),
                rs.getString("input_text"),
                rs.getString("tech_stack"),
                getNullableLong(rs, "matched_error_id"),
                rs.getString("matched_error_name"),
                getNullableLong(rs, "matched_error_id") != null,
                rs.getString("ai_advice"),
                graph.nodes().size(),
                graph.links().size(),
                rs.getTimestamp("create_time").toLocalDateTime()
        );
    }

    private AnalysisHistoryDetail mapDetail(ResultSet rs) throws SQLException {
        Long matchedErrorId = getNullableLong(rs, "matched_error_id");
        return new AnalysisHistoryDetail(
                rs.getLong("id"),
                getNullableLong(rs, "user_id"),
                rs.getString("username"),
                rs.getString("nickname"),
                rs.getString("input_text"),
                rs.getString("tech_stack"),
                matchedErrorId,
                rs.getString("matched_error_name"),
                matchedErrorId != null,
                fromJson(rs.getString("graph_result")),
                rs.getString("ai_advice"),
                rs.getTimestamp("create_time").toLocalDateTime()
        );
    }

    private String toJson(GraphResponse graph) {
        try {
            return objectMapper.writeValueAsString(graph);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize graph result", exception);
        }
    }

    private GraphResponse fromJson(String graphJson) {
        if (!hasText(graphJson)) {
            return new GraphResponse(List.of(), List.of());
        }
        try {
            return objectMapper.readValue(graphJson, GraphResponse.class);
        } catch (JsonProcessingException exception) {
            return new GraphResponse(List.of(), List.of());
        }
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
