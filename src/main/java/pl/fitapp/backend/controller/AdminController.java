package pl.fitapp.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.fitapp.backend.service.UsdaImportService;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UsdaImportService usdaImportService;

    @PostMapping("/import-usda")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> importUsda(@RequestParam String path) {
        try {
            log.info("Starting USDA import from path: {}", path);
            usdaImportService.importFromCsv(path);
            return ResponseEntity.ok(Map.of("status", "success", "message", "USDA data imported successfully"));
        } catch (Exception e) {
            log.error("Failed to import USDA data", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
