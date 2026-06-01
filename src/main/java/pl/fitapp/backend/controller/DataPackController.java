package pl.fitapp.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.fitapp.backend.dto.DataPackVersionDTO;
import pl.fitapp.backend.service.DataPackService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/packs")
@RequiredArgsConstructor
public class DataPackController {

    private final DataPackService dataPackService;

    @GetMapping("/versions")
    public ResponseEntity<List<DataPackVersionDTO>> versions() {
        return ResponseEntity.ok(dataPackService.getVersions());
    }

    @GetMapping("/dish-templates")
    public ResponseEntity<Map<String, Object>> dishTemplates(
            @RequestParam(defaultValue = "0") int since
    ) {
        return ResponseEntity.ok(dataPackService.exportDishTemplates(since));
    }

    @GetMapping("/dictionary/{lang}")
    public ResponseEntity<Map<String, Object>> dictionary(
            @PathVariable String lang,
            @RequestParam(defaultValue = "0") int since
    ) {
        return ResponseEntity.ok(dataPackService.exportDictionary(lang, since));
    }

    @GetMapping("/mini-usda")
    public ResponseEntity<Map<String, Object>> miniUsda(
            @RequestParam(defaultValue = "0") int since
    ) {
        return ResponseEntity.ok(dataPackService.exportMiniUsda(since));
    }
}
