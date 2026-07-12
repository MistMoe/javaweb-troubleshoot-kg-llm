package com.mistmoe.troubleshoot.auth;

public record PasswordChangeRequest(String currentPassword, String newPassword) {
}
