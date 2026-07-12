package com.mistmoe.troubleshoot.user;

import com.mistmoe.troubleshoot.auth.RoleInfo;

import java.time.LocalDateTime;
import java.util.List;

public record ManagedUser(
        Long id,
        String username,
        String nickname,
        String email,
        Integer status,
        List<RoleInfo> roles,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
