package com.mistmoe.troubleshoot.user;

import com.mistmoe.troubleshoot.auth.RoleInfo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserAdminRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserAdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ManagedUser> list(String keyword, String roleCode, Integer status) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT u.id, u.username, u.nickname, u.email, u.status, u.create_time, u.update_time
                FROM sys_user u
                LEFT JOIN sys_user_role ur ON ur.user_id = u.id
                LEFT JOIN sys_role r ON r.id = ur.role_id
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (hasText(keyword)) {
            sql.append(" AND (u.username LIKE ? OR u.nickname LIKE ? OR u.email LIKE ?) ");
            String pattern = "%" + keyword.trim() + "%";
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (hasText(roleCode)) {
            sql.append(" AND r.role_code = ? ");
            args.add(roleCode.trim());
        }
        if (status != null) {
            sql.append(" AND u.status = ? ");
            args.add(status);
        }
        sql.append(" ORDER BY u.id");
        List<ManagedUser> users = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapUser(rs, List.of()), args.toArray());
        return users.stream()
                .map(user -> withRoles(user, findRolesByUserId(user.id())))
                .toList();
    }

    public List<RoleInfo> listRoles() {
        String sql = """
                SELECT id, role_code, role_name, description
                FROM sys_role
                ORDER BY id
                """;
        return jdbcTemplate.query(sql, this::mapRole);
    }

    public Optional<ManagedUser> findById(Long id) {
        String sql = """
                SELECT id, username, nickname, email, status, create_time, update_time
                FROM sys_user
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapUser(rs, findRolesByUserId(id)), id)
                .stream()
                .findFirst();
    }

    public ManagedUser create(UserAdminRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO sys_user (username, password, nickname, email, status)
                        VALUES (?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, request.username().trim());
                statement.setString(2, request.password().trim());
                statement.setString(3, trimToNull(request.nickname()));
                statement.setString(4, trimToNull(request.email()));
                statement.setInt(5, normalizeStatus(request.status()));
                return statement;
            }, keyHolder);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Long id = extractGeneratedId(keyHolder);
        replaceRoles(id, request.roleIds());
        return findById(id).orElseThrow();
    }

    public Optional<ManagedUser> update(Long id, UserAdminRequest request) {
        if (hasText(request.password())) {
            jdbcTemplate.update("""
                    UPDATE sys_user
                    SET username = ?, password = ?, nickname = ?, email = ?, status = ?, update_time = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """,
                    request.username().trim(),
                    request.password().trim(),
                    trimToNull(request.nickname()),
                    trimToNull(request.email()),
                    normalizeStatus(request.status()),
                    id
            );
        } else {
            jdbcTemplate.update("""
                    UPDATE sys_user
                    SET username = ?, nickname = ?, email = ?, status = ?, update_time = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """,
                    request.username().trim(),
                    trimToNull(request.nickname()),
                    trimToNull(request.email()),
                    normalizeStatus(request.status()),
                    id
            );
        }
        replaceRoles(id, request.roleIds());
        return findById(id);
    }

    public Optional<ManagedUser> updateStatus(Long id, Integer status) {
        jdbcTemplate.update("""
                UPDATE sys_user
                SET status = ?, update_time = CURRENT_TIMESTAMP
                WHERE id = ?
                """, normalizeStatus(status), id);
        return findById(id);
    }

    private List<RoleInfo> findRolesByUserId(Long userId) {
        String sql = """
                SELECT r.id, r.role_code, r.role_name, r.description
                FROM sys_role r
                INNER JOIN sys_user_role ur ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.id
                """;
        return jdbcTemplate.query(sql, this::mapRole, userId);
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        for (Long roleId : roleIds) {
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        }
    }

    private ManagedUser mapUser(ResultSet rs, List<RoleInfo> roles) throws SQLException {
        return new ManagedUser(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("nickname"),
                rs.getString("email"),
                rs.getInt("status"),
                roles,
                rs.getTimestamp("create_time").toLocalDateTime(),
                rs.getTimestamp("update_time").toLocalDateTime()
        );
    }

    private ManagedUser withRoles(ManagedUser user, List<RoleInfo> roles) {
        return new ManagedUser(
                user.id(),
                user.username(),
                user.nickname(),
                user.email(),
                user.status(),
                roles,
                user.createTime(),
                user.updateTime()
        );
    }

    private RoleInfo mapRole(ResultSet rs, int rowNum) throws SQLException {
        return new RoleInfo(
                rs.getLong("id"),
                rs.getString("role_code"),
                rs.getString("role_name"),
                rs.getString("description")
        );
    }

    private Long extractGeneratedId(KeyHolder keyHolder) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            Object id = keys.get("id");
            if (id == null) {
                id = keys.get("ID");
            }
            if (id instanceof Number number) {
                return number.longValue();
            }
        }
        if (keyHolder.getKeyList().size() == 1 && keyHolder.getKeyList().get(0).size() == 1) {
            Object value = keyHolder.getKeyList().get(0).values().iterator().next();
            if (value instanceof Number number) {
                return number.longValue();
            }
        }
        throw new IllegalStateException("Failed to read generated user id");
    }

    private int normalizeStatus(Integer status) {
        return status != null && status == 0 ? 0 : 1;
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
