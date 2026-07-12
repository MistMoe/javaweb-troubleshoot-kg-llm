package com.mistmoe.troubleshoot.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserAccount> findEnabledUserByUsername(String username) {
        String sql = """
                SELECT id, username, password, nickname, email, status
                FROM sys_user
                WHERE username = ? AND status = 1
                """;
        List<UserAccount> users = jdbcTemplate.query(sql, this::mapUser, username);
        return users.stream().findFirst();
    }

    public Optional<UserAccount> findUserById(Long userId) {
        String sql = """
                SELECT id, username, password, nickname, email, status
                FROM sys_user
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, this::mapUser, userId).stream().findFirst();
    }

    public Optional<UserAccount> updateProfile(Long userId, String nickname, String email) {
        jdbcTemplate.update("""
                UPDATE sys_user
                SET nickname = ?, email = ?, update_time = CURRENT_TIMESTAMP
                WHERE id = ?
                """, nickname, email, userId);
        return findUserById(userId);
    }

    public boolean passwordMatches(Long userId, String password) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user
                WHERE id = ? AND password = ?
                """, Integer.class, userId, password);
        return count != null && count == 1;
    }

    public boolean updatePassword(Long userId, String password) {
        return jdbcTemplate.update("""
                UPDATE sys_user
                SET password = ?, update_time = CURRENT_TIMESTAMP
                WHERE id = ?
                """, password, userId) == 1;
    }

    public List<RoleInfo> findRolesByUserId(Long userId) {
        String sql = """
                SELECT r.id, r.role_code, r.role_name, r.description
                FROM sys_role r
                INNER JOIN sys_user_role ur ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.id
                """;
        return jdbcTemplate.query(sql, this::mapRole, userId);
    }

    private UserAccount mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new UserAccount(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("nickname"),
                rs.getString("email"),
                rs.getInt("status")
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
}
