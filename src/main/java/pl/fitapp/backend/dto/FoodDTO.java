package pl.fitapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodDTO {
    private String id;
    private String externalId;
    private String source;
    private String barcode;
    private String name;
    private String brand;
    private BigDecimal servingSize;
    private String servingUnit;

    private BigDecimal calories;
    private BigDecimal protein;
    private BigDecimal fat;
    private BigDecimal carbohydrates;
    private BigDecimal fiber;
    private BigDecimal sugar;
    private BigDecimal sodium;

    private String language;
    private String countryCodes;
    private String imageUrl;
}
