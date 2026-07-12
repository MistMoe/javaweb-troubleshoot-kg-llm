package com.mistmoe.troubleshoot.favorite;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FavoriteRepository {

    private final JdbcTemplate jdbcTemplate;

    public FavoriteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FavoriteCase> listByUser(Long userId) {
        String sql = """
                SELECT f.id, f.user_id, f.error_case_id, f.create_time,
                       e.error_name, e.stage, e.component, e.keyword
                FROM error_case_favorite f
                INNER JOIN error_case e ON e.id = f.error_case_id
                WHERE f.user_id = ?
                ORDER BY f.create_time DESC, f.id DESC
                """;
        return jdbcTemplate.query(sql, this::mapRow, userId);
    }

    public List<Long> findCaseIds(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT error_case_id FROM error_case_favorite WHERE user_id = ? ORDER BY create_time DESC, id DESC",
                Long.class,
                userId
        );
    }

    public boolean add(Long userId, Long caseId) {
        if (exists(userId, caseId)) {
            return false;
        }
        return jdbcTemplate.update(
                "INSERT INTO error_case_favorite (user_id, error_case_id) VALUES (?, ?)",
                userId,
                caseId
        ) > 0;
    }

    public boolean remove(Long userId, Long caseId) {
        return jdbcTemplate.update(
                "DELETE FROM error_case_favorite WHERE user_id = ? AND error_case_id = ?",
                userId,
                caseId
        ) > 0;
    }

    public boolean exists(Long userId, Long caseId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM error_case_favorite WHERE user_id = ? AND error_case_id = ?",
                Integer.class,
                userId,
                caseId
        );
        return count != null && count > 0;
    }

    private FavoriteCase mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new FavoriteCase(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("error_case_id"),
                rs.getString("error_name"),
                rs.getString("stage"),
                rs.getString("component"),
                rs.getString("keyword"),
                rs.getTimestamp("create_time").toLocalDateTime()
        );
    }
}
