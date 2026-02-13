package pl.fitapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MealRequest {

    @NotBlank
    private String name;

    @NotNull
    @PositiveOrZero
    private BigDecimal calories;

    @PositiveOrZero
    private BigDecimal proteins;

    @PositiveOrZero
    private BigDecimal fats;

    @PositiveOrZero
    private BigDecimal carbohydrates;

    @NotNull
    private LocalDateTime eatenAt;
}
