package com.mistmoe.troubleshoot.submission;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeSubmissionRepository {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeSubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public KnowledgeSubmission create(KnowledgeSubmissionRequest request, KnowledgeSubmissionDraft draft) {
        String sql = """
                INSERT INTO error_case_submission (
                    submit_user_id, raw_log, tech_stack, stage, user_solution,
                    related_file, related_command, draft_error_name, draft_keyword,
                    draft_symptom, draft_cause, draft_solution, draft_related_file,
                    draft_related_command, draft_risk_tip, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, request.submitUserId());
            ps.setString(2, request.rawLog());
            ps.setString(3, request.techStack());
            ps.setString(4, request.stage());
            ps.setString(5, request.userSolution());
            ps.setString(6, request.relatedFile());
            ps.setString(7, request.relatedCommand());
            ps.setString(8, draft.errorName());
            ps.setString(9, draft.keyword());
            ps.setString(10, draft.symptom());
            ps.setString(11, draft.cause());
            ps.setString(12, draft.solution());
            ps.setString(13, draft.relatedFile());
            ps.setString(14, draft.relatedCommand());
            ps.setString(15, draft.riskTip());
            ps.setString(16, STATUS_PENDING);
            return ps;
        }, keyHolder);
        Number id = extractGeneratedId(keyHolder);
        return findById(id == null ? 0L : id.longValue()).orElseThrow();
    }

    public List<KnowledgeSubmission> list(String status, Long submitUserId) {
        StringBuilder sql = new StringBuilder(baseSelect()).append(" WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (hasText(status)) {
            sql.append(" AND s.status = ? ");
            args.add(status.trim());
        }
        if (submitUserId != null) {
            sql.append(" AND s.submit_user_id = ? ");
            args.add(submitUserId);
        }
        sql.append(" ORDER BY s.id DESC");
        return jdbcTemplate.query(sql.toString(), this::mapRow, args.toArray());
    }

    public Optional<KnowledgeSubmission> findById(Long id) {
        String sql = baseSelect() + " WHERE s.id = ?";
        List<KnowledgeSubmission> rows = jdbcTemplate.query(sql, this::mapRow, id);
        return rows.stream().findFirst();
    }

    public boolean reviewerCanApprove(Long reviewerId) {
        if (reviewerId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_role ur
                INNER JOIN sys_role r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND r.role_code IN ('ADMIN', 'MAINTAINER')
                """, Integer.class, reviewerId);
        return count != null && count > 0;
    }

    public Optional<KnowledgeSubmission> approve(Long id, Long reviewerId, String reviewComment,
                                                 KnowledgeSubmissionDraft draft, Long approvedErrorCaseId) {
        int updated = jdbcTemplate.update("""
                UPDATE error_case_submission
                SET draft_error_name = ?, draft_keyword = ?, draft_symptom = ?, draft_cause = ?,
                    draft_solution = ?, draft_related_file = ?, draft_related_command = ?, draft_risk_tip = ?,
                    status = ?, reviewer_id = ?, review_comment = ?, approved_error_case_id = ?,
                    update_time = CURRENT_TIMESTAMP
                WHERE id = ? AND status = ?
                """,
                draft.errorName(),
                draft.keyword(),
                draft.symptom(),
                draft.cause(),
                draft.solution(),
                draft.relatedFile(),
                draft.relatedCommand(),
                draft.riskTip(),
                STATUS_APPROVED,
                reviewerId,
                reviewComment,
                approvedErrorCaseId,
                id,
                STATUS_PENDING
        );
        return updated == 0 ? Optional.empty() : findById(id);
    }

    public Optional<KnowledgeSubmission> reject(Long id, Long reviewerId, String reviewComment) {
        int updated = jdbcTemplate.update("""
                UPDATE error_case_submission
                SET status = ?, reviewer_id = ?, review_comment = ?, update_time = CURRENT_TIMESTAMP
                WHERE id = ? AND status = ?
                """, STATUS_REJECTED, reviewerId, reviewComment, id, STATUS_PENDING);
        return updated == 0 ? Optional.empty() : findById(id);
    }

    private String baseSelect() {
        return """
                SELECT s.id, s.submit_user_id, submit_user.username AS submit_username,
                       submit_user.nickname AS submit_nickname, s.raw_log, s.tech_stack, s.stage,
                       s.user_solution, s.related_file, s.related_command, s.draft_error_name,
                       s.draft_keyword, s.draft_symptom, s.draft_cause, s.draft_solution,
                       s.draft_related_file, s.draft_related_command, s.draft_risk_tip, s.status,
                       s.reviewer_id, reviewer.username AS reviewer_username,
                       reviewer.nickname AS reviewer_nickname, s.review_comment,
                       s.approved_error_case_id, s.create_time, s.update_time
                FROM error_case_submission s
                LEFT JOIN sys_user submit_user ON submit_user.id = s.submit_user_id
                LEFT JOIN sys_user reviewer ON reviewer.id = s.reviewer_id
                """;
    }

    private KnowledgeSubmission mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new KnowledgeSubmission(
                rs.getLong("id"),
                getNullableLong(rs, "submit_user_id"),
                rs.getString("submit_username"),
                rs.getString("submit_nickname"),
                rs.getString("raw_log"),
                rs.getString("tech_stack"),
                rs.getString("stage"),
                rs.getString("user_solution"),
                rs.getString("related_file"),
                rs.getString("related_command"),
                rs.getString("draft_error_name"),
                rs.getString("draft_keyword"),
                rs.getString("draft_symptom"),
                rs.getString("draft_cause"),
                rs.getString("draft_solution"),
                rs.getString("draft_related_file"),
                rs.getString("draft_related_command"),
                rs.getString("draft_risk_tip"),
                rs.getString("status"),
                getNullableLong(rs, "reviewer_id"),
                rs.getString("reviewer_username"),
                rs.getString("reviewer_nickname"),
                rs.getString("review_comment"),
                getNullableLong(rs, "approved_error_case_id"),
                toLocalDateTime(rs.getTimestamp("create_time")),
                toLocalDateTime(rs.getTimestamp("update_time"))
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
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
            // H2/MySQL may return generated timestamp columns beside id.
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
