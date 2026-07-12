package com.mistmoe.troubleshoot.auth;

import java.util.List;

public record UserProfile(Long id, String username, String nickname, String email, Integer status, List<RoleInfo> roles) {

    public static UserProfile from(UserAccount user, List<RoleInfo> roles) {
        return new UserProfile(user.id(), user.username(), user.nickname(), user.email(), user.status(), roles);
    }
}
