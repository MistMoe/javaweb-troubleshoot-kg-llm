package com.mistmoe.troubleshoot.user;

import com.mistmoe.troubleshoot.auth.RoleInfo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private final UserAdminRepository repository;

    public UserAdminService(UserAdminRepository repository) {
        this.repository = repository;
    }

    public List<ManagedUser> list(String keyword, String roleCode, Integer status) {
        return repository.list(keyword, roleCode, status);
    }

    public List<RoleInfo> listRoles() {
        return repository.listRoles();
    }

    public ManagedUser create(UserAdminRequest request) {
        validate(request, true);
        return repository.create(request);
    }

    public Optional<ManagedUser> update(Long id, UserAdminRequest request) {
        validateId(id);
        validate(request, false);
        if (repository.findById(id).isEmpty()) {
            return Optional.empty();
        }
        return repository.update(id, request);
    }

    public Optional<ManagedUser> updateStatus(Long id, UserStatusRequest request) {
        validateId(id);
        if (request == null || request.status() == null || (request.status() != 0 && request.status() != 1)) {
            throw new IllegalArgumentException("用户状态只能是 0 或 1");
        }
        if (repository.findById(id).isEmpty()) {
            return Optional.empty();
        }
        return repository.updateStatus(id, request.status());
    }

    private void validate(UserAdminRequest request, boolean creating) {
        if (request == null || !hasText(request.username())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (creating && !hasText(request.password())) {
            throw new IllegalArgumentException("新建用户必须填写密码");
        }
        if (request.status() != null && request.status() != 0 && request.status() != 1) {
            throw new IllegalArgumentException("用户状态只能是 0 或 1");
        }
        if (request.roleIds() == null || request.roleIds().isEmpty()) {
            throw new IllegalArgumentException("至少选择一个角色");
        }
        Set<Long> validRoleIds = listRoles().stream().map(RoleInfo::id).collect(Collectors.toSet());
        if (!validRoleIds.containsAll(request.roleIds())) {
            throw new IllegalArgumentException("角色不存在");
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("用户 ID 不合法");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
