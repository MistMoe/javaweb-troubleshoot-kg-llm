package com.mistmoe.troubleshoot.user;

import java.util.List;

public record UserAdminRequest(
        String username,
        String password,
        String nickname,
        String email,
        Integer status,
        List<Long> roleIds
) {
}
