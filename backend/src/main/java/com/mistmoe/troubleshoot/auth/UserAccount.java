package com.mistmoe.troubleshoot.auth;

public record UserAccount(Long id, String username, String password, String nickname, String email, Integer status) {
}
