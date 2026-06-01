package pl.fitapp.backend.service;

import pl.fitapp.backend.dto.TemplateIngredientDTO;

import java.util.List;

public record VisionResult(
        String dish,
        String dishKey,
        List<TemplateIngredientDTO> templateIngredients,
        List<String> ingredients,
        String provider
) {
    public static VisionResult empty() {
        return new VisionResult(null, null, List.of(), List.of(), "none");
    }

    public static VisionResult ingredientsOnly(List<String> ingredients, String provider) {
        return new VisionResult(null, null, List.of(), ingredients, provider);
    }
}
