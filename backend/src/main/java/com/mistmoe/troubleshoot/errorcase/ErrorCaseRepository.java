package com.mistmoe.troubleshoot.errorcase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ErrorCaseRepository {

    private final JdbcTemplate jdbcTemplate;

    public ErrorCaseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ErrorCase> list(String keyword, String component, String stage) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, error_name, stage, component, keyword, symptom, cause, solution,
                       related_file, related_command, risk_tip, create_user_id, update_user_id,
                       create_time, update_time
                FROM error_case
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();

        if (hasText(keyword)) {
            sql.append("""
                    AND (
                        error_name LIKE ?
                        OR keyword LIKE ?
                        OR symptom LIKE ?
                        OR cause LIKE ?
                        OR solution LIKE ?
                    )
                    """);
            String pattern = "%" + keyword.trim() + "%";
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (hasText(component)) {
            sql.append(" AND component = ? ");
            args.add(component.trim());
        }
        if (hasText(stage)) {
            sql.append(" AND stage = ? ");
            args.add(stage.trim());
        }

        sql.append(" ORDER BY id DESC");
        return jdbcTemplate.query(sql.toString(), this::mapRow, args.toArray());
    }

    public Optional<ErrorCase> findById(Long id) {
        String sql = """
                SELECT id, error_name, stage, component, keyword, symptom, cause, solution,
                       related_file, related_command, risk_tip, create_user_id, update_user_id,
                       create_time, update_time
                FROM error_case
                WHERE id = ?
                """;
        List<ErrorCase> rows = jdbcTemplate.query(sql, this::mapRow, id);
        return rows.stream().findFirst();
    }

    public ErrorCase create(ErrorCaseRequest request) {
        String sql = """
                INSERT INTO error_case (
                    error_name, stage, component, keyword, symptom, cause, solution,
                    related_file, related_command, risk_tip, create_user_id, update_user_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            bindRequest(ps, request, false);
            return ps;
        }, keyHolder);
        Number key = extractGeneratedId(keyHolder);
        return findById(key == null ? 0L : key.longValue()).orElseThrow();
    }

    public Optional<ErrorCase> update(Long id, ErrorCaseRequest request) {
        String sql = """
                UPDATE error_case
                SET error_name = ?, stage = ?, component = ?, keyword = ?, symptom = ?,
                    cause = ?, solution = ?, related_file = ?, related_command = ?,
                    risk_tip = ?, update_user_id = ?
                WHERE id = ?
                """;
        int updated = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            bindRequest(ps, request, true);
            ps.setLong(12, id);
            return ps;
        });
        if (updated == 0) {
            return Optional.empty();
        }
        return findById(id);
    }

    public boolean delete(Long id) {
        return jdbcTemplate.update("DELETE FROM error_case WHERE id = ?", id) > 0;
    }

    private void bindRequest(PreparedStatement ps, ErrorCaseRequest request, boolean updateOnly) throws SQLException {
        ps.setString(1, request.errorName());
        ps.setString(2, request.stage());
        ps.setString(3, request.component());
        ps.setString(4, request.keyword());
        ps.setString(5, request.symptom());
        ps.setString(6, request.cause());
        ps.setString(7, request.solution());
        ps.setString(8, request.relatedFile());
        ps.setString(9, request.relatedCommand());
        ps.setString(10, request.riskTip());
        if (updateOnly) {
            ps.setObject(11, request.userId());
        } else {
            ps.setObject(11, request.userId());
            ps.setObject(12, request.userId());
        }
    }

    private ErrorCase mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ErrorCase(
                rs.getLong("id"),
                rs.getString("error_name"),
                rs.getString("stage"),
                rs.getString("component"),
                rs.getString("keyword"),
                rs.getString("symptom"),
                rs.getString("cause"),
                rs.getString("solution"),
                rs.getString("related_file"),
                rs.getString("related_command"),
                rs.getString("risk_tip"),
                getNullableLong(rs, "create_user_id"),
                getNullableLong(rs, "update_user_id"),
                rs.getTimestamp("create_time").toLocalDateTime(),
                rs.getTimestamp("update_time").toLocalDateTime()
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Number extractGeneratedId(KeyHolder keyHolder) {
        Number singleKey = null;
        try {
            singleKey = keyHolder.getKey();
        } catch (InvalidDataAccessApiUsageException ignored) {
            // Some databases return generated timestamp columns beside id.
        }
        if (singleKey != null) {
            return singleKey;
        }
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys == null) {
            return null;
        }
        Object id = keys.getOrDefault("id", keys.get("ID"));
        return id instanceof Number number ? number : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
