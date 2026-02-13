package pl.fitapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DishTemplateDTO {
    private String dishKey;
    private String nameEn;
    private String namePl;
    private String category;
    private BigDecimal defaultServingG;
    private List<DishTemplateIngredientDTO> ingredients;
}
