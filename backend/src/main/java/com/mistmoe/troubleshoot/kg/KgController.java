package com.mistmoe.troubleshoot.kg;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kg")
public class KgController {

    private final KgService kgService;

    public KgController(KgService kgService) {
        this.kgService = kgService;
    }

    @GetMapping("/graph/{errorId}")
    public GraphResponse graph(@PathVariable Long errorId) {
        return kgService.graphByErrorId(errorId);
    }

    @GetMapping("/search")
    public GraphResponse search(@RequestParam String keyword) {
        return kgService.search(keyword);
    }

    @PostMapping("/import")
    public KgImportResponse importGraph() {
        return kgService.importFromMysql();
    }
}
