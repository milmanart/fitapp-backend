package pl.fitapp.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.fitapp.backend.dto.VisionRequest;
import pl.fitapp.backend.dto.VisionResponse;
import pl.fitapp.backend.service.VisionResult;
import pl.fitapp.backend.service.VisionService;

@RestController
@RequestMapping("/api/vision")
@RequiredArgsConstructor
public class VisionController {

    private final VisionService visionService;

    @PostMapping("/analyze")
    public ResponseEntity<VisionResponse> analyze(@Valid @RequestBody VisionRequest request) {
        VisionResult result = visionService.identifyFoods(request.getImageBase64(), request.getMimeType());
        return ResponseEntity.ok(new VisionResponse(
                result.dish(),
                result.dishKey(),
                result.templateIngredients(),
                result.ingredients(),
                result.provider()
        ));
    }
}
