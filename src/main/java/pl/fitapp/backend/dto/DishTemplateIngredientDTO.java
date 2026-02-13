package pl.fitapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DishTemplateIngredientDTO {
    private String foodKey;
    private String nameEn;
    private String namePl;
    private BigDecimal defaultAmountG;
    private BigDecimal proportionPercent;
    private int sortOrder;
}
