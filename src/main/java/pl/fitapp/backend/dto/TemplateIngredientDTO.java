package pl.fitapp.backend.dto;

public record TemplateIngredientDTO(
        String foodKey,
        String nameEn,
        String namePl,
        double defaultAmountG
) {}
