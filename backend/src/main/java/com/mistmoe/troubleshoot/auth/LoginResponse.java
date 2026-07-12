package com.mistmoe.troubleshoot.auth;

public record LoginResponse(String token, UserProfile user) {
}
