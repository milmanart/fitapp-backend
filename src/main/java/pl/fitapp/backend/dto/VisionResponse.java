package pl.fitapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class VisionResponse {
    private String dish;
    private String dishKey;
    private List<TemplateIngredientDTO> templateIngredients;
    private List<String> ingredients;
    private String provider;
}
