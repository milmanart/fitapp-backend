package pl.fitapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodLocalDTO {
    private String foodKey;
    private String nameEn;
    private String namePl;

    private BigDecimal caloriesPer100g;
    private BigDecimal proteinPer100g;
    private BigDecimal fatPer100g;
    private BigDecimal carbsPer100g;

    private String source;
}
