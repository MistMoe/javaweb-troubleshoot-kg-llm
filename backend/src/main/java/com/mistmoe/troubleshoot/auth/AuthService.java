package com.mistmoe.troubleshoot.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AuthRepository authRepository;
    private final ConcurrentMap<String, UserProfile> sessions = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public Optional<LoginResponse> login(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.username()) || !StringUtils.hasText(request.password())) {
            return Optional.empty();
        }

        return authRepository.findEnabledUserByUsername(request.username().trim())
                .filter(user -> user.password().equals(request.password()))
                .map(user -> {
                    List<RoleInfo> roles = authRepository.findRolesByUserId(user.id());
                    UserProfile profile = UserProfile.from(user, roles);
                    String token = createToken();
                    sessions.put(token, profile);
                    return new LoginResponse(token, profile);
                });
    }

    public Optional<UserProfile> profile(String authorization) {
        String token = extractBearerToken(authorization);
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(token));
    }

    public Optional<UserProfile> updateProfile(String authorization, ProfileUpdateRequest request) {
        String token = extractBearerToken(authorization);
        UserProfile current = sessions.get(token);
        if (current == null) {
            return Optional.empty();
        }
        if (request == null) {
            throw new IllegalArgumentException("资料内容不能为空");
        }

        String nickname = trimToNull(request.nickname());
        String email = trimToNull(request.email());
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }

        UserAccount account = authRepository.updateProfile(current.id(), nickname, email)
                .orElseThrow(() -> new IllegalArgumentException("当前账号不存在"));
        UserProfile updated = UserProfile.from(account, current.roles());
        sessions.put(token, updated);
        return Optional.of(updated);
    }

    public Optional<PasswordChangeResponse> changePassword(String authorization, PasswordChangeRequest request) {
        String token = extractBearerToken(authorization);
        UserProfile current = sessions.get(token);
        if (current == null) {
            return Optional.empty();
        }
        if (request == null || !StringUtils.hasText(request.currentPassword())
                || !StringUtils.hasText(request.newPassword())) {
            throw new IllegalArgumentException("请完整填写原密码和新密码");
        }

        String newPassword = request.newPassword().trim();
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码不能少于 6 位");
        }
        if (request.currentPassword().equals(newPassword)) {
            throw new IllegalArgumentException("新密码不能与原密码相同");
        }
        if (!authRepository.passwordMatches(current.id(), request.currentPassword())) {
            throw new IllegalArgumentException("原密码不正确");
        }
        if (!authRepository.updatePassword(current.id(), newPassword)) {
            throw new IllegalArgumentException("密码修改失败");
        }
        return Optional.of(new PasswordChangeResponse("密码修改成功"));
    }

    private String createToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return "";
        }
        String prefix = "Bearer ";
        if (!authorization.startsWith(prefix)) {
            return "";
        }
        return authorization.substring(prefix.length()).trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
