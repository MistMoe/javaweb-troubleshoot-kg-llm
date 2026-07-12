package com.mistmoe.troubleshoot.favorite;

import java.time.LocalDateTime;

public record FavoriteCase(
        Long id,
        Long userId,
        Long errorCaseId,
        String errorName,
        String stage,
        String component,
        String keyword,
        LocalDateTime createTime
) {
}
