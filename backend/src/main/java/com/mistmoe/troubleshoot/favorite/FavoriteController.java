package com.mistmoe.troubleshoot.favorite;

import java.util.Map;

import com.mistmoe.troubleshoot.auth.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/cases")
    public Map<String, Object> list(@RequestParam Long userId) {
        return Map.of("items", favoriteService.listByUser(userId));
    }

    @GetMapping("/case-ids")
    public Map<String, Object> caseIds(@RequestParam Long userId) {
        return Map.of("ids", favoriteService.findCaseIds(userId));
    }

    @PostMapping("/cases/{caseId}")
    public Map<String, Object> add(@PathVariable Long caseId, @RequestParam Long userId) {
        return Map.of("favorited", true, "created", favoriteService.add(userId, caseId));
    }

    @DeleteMapping("/cases/{caseId}")
    public Map<String, Object> remove(@PathVariable Long caseId, @RequestParam Long userId) {
        return Map.of("favorited", false, "deleted", favoriteService.remove(userId, caseId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }
}
