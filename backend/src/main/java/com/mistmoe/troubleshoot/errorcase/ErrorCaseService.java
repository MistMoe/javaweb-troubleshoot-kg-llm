package com.mistmoe.troubleshoot.errorcase;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ErrorCaseService {

    private final ErrorCaseRepository errorCaseRepository;

    public ErrorCaseService(ErrorCaseRepository errorCaseRepository) {
        this.errorCaseRepository = errorCaseRepository;
    }

    public List<ErrorCase> list(String keyword, String component, String stage) {
        return errorCaseRepository.list(keyword, component, stage);
    }

    public ErrorCase create(ErrorCaseRequest request) {
        validate(request);
        return errorCaseRepository.create(request);
    }

    public Optional<ErrorCase> update(Long id, ErrorCaseRequest request) {
        validate(request);
        return errorCaseRepository.update(id, request);
    }

    public boolean delete(Long id) {
        return errorCaseRepository.delete(id);
    }

    private void validate(ErrorCaseRequest request) {
        if (request == null || !StringUtils.hasText(request.errorName())) {
            throw new IllegalArgumentException("故障名称不能为空");
        }
        if (!StringUtils.hasText(request.keyword())) {
            throw new IllegalArgumentException("匹配关键词不能为空");
        }
    }
}
