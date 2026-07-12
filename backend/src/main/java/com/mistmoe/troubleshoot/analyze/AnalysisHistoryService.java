package com.mistmoe.troubleshoot.analyze;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class AnalysisHistoryService {

    private final AnalysisHistoryRepository repository;

    public AnalysisHistoryService(AnalysisHistoryRepository repository) {
        this.repository = repository;
    }

    public List<AnalysisHistorySummary> list(Long userId, String keyword, Boolean matched) {
        return repository.list(userId, keyword, matched);
    }

    public Optional<AnalysisHistoryDetail> findById(Long id) {
        return repository.findById(id);
    }

    public boolean delete(Long id) {
        return repository.delete(id);
    }
}
