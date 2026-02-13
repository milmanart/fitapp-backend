package pl.fitapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyNutritionResponse {
    private LocalDate date;
    private BigDecimal totalCalories;
    private BigDecimal totalProteins;
    private BigDecimal totalFats;
    private BigDecimal totalCarbohydrates;
    private int mealCount;
}
