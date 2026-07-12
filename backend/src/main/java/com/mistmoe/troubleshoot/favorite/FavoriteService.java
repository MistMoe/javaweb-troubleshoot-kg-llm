package com.mistmoe.troubleshoot.favorite;

import java.util.List;

import com.mistmoe.troubleshoot.errorcase.ErrorCaseRepository;
import org.springframework.stereotype.Service;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ErrorCaseRepository errorCaseRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, ErrorCaseRepository errorCaseRepository) {
        this.favoriteRepository = favoriteRepository;
        this.errorCaseRepository = errorCaseRepository;
    }

    public List<FavoriteCase> listByUser(Long userId) {
        requireUser(userId);
        return favoriteRepository.listByUser(userId);
    }

    public List<Long> findCaseIds(Long userId) {
        requireUser(userId);
        return favoriteRepository.findCaseIds(userId);
    }

    public boolean add(Long userId, Long caseId) {
        requireUser(userId);
        requireCase(caseId);
        return favoriteRepository.add(userId, caseId);
    }

    public boolean remove(Long userId, Long caseId) {
        requireUser(userId);
        requireCase(caseId);
        return favoriteRepository.remove(userId, caseId);
    }

    private void requireUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private void requireCase(Long caseId) {
        if (caseId == null || caseId <= 0) {
            throw new IllegalArgumentException("caseId is required");
        }
        if (errorCaseRepository.findById(caseId).isEmpty()) {
            throw new IllegalArgumentException("故障案例不存在");
        }
    }
}
