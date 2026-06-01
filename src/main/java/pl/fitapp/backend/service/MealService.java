package pl.fitapp.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.fitapp.backend.domain.Meal;
import pl.fitapp.backend.dto.DailyNutritionResponse;
import pl.fitapp.backend.dto.MealRequest;
import pl.fitapp.backend.dto.MealResponse;
import pl.fitapp.backend.repository.MealRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealRepository mealRepository;

    @Transactional
    public MealResponse createMeal(UUID userId, MealRequest request) {
        Meal meal = Meal.builder()
                .userId(userId)
                .name(request.getName())
                .calories(request.getCalories())
                .proteins(request.getProteins() != null ? request.getProteins() : BigDecimal.ZERO)
                .fats(request.getFats() != null ? request.getFats() : BigDecimal.ZERO)
                .carbohydrates(request.getCarbohydrates() != null ? request.getCarbohydrates() : BigDecimal.ZERO)
                .eatenAt(request.getEatenAt())
                .build();

        Meal savedMeal = mealRepository.save(meal);
        return toResponse(savedMeal);
    }

    @Transactional(readOnly = true)
    public List<MealResponse> getMealsByUserId(UUID userId) {
        return mealRepository.findByUserIdOrderByEatenAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MealResponse> getMealsByUserIdAndDate(UUID userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return mealRepository.findByUserIdAndEatenAtBetweenOrderByEatenAtDesc(userId, startOfDay, endOfDay)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DailyNutritionResponse getDailyNutrition(UUID userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Meal> meals = mealRepository.findByUserIdAndEatenAtBetweenOrderByEatenAtDesc(userId, startOfDay, endOfDay);

        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal totalProteins = BigDecimal.ZERO;
        BigDecimal totalFats = BigDecimal.ZERO;
        BigDecimal totalCarbs = BigDecimal.ZERO;

        for (Meal meal : meals) {
            totalCalories = totalCalories.add(meal.getCalories() != null ? meal.getCalories() : BigDecimal.ZERO);
            totalProteins = totalProteins.add(meal.getProteins() != null ? meal.getProteins() : BigDecimal.ZERO);
            totalFats = totalFats.add(meal.getFats() != null ? meal.getFats() : BigDecimal.ZERO);
            totalCarbs = totalCarbs.add(meal.getCarbohydrates() != null ? meal.getCarbohydrates() : BigDecimal.ZERO);
        }

        return DailyNutritionResponse.builder()
                .date(date)
                .totalCalories(totalCalories)
                .totalProteins(totalProteins)
                .totalFats(totalFats)
                .totalCarbohydrates(totalCarbs)
                .mealCount(meals.size())
                .build();
    }

    @Transactional
    public MealResponse updateMeal(UUID mealId, UUID userId, MealRequest request) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new pl.fitapp.backend.exception.NotFoundException("Meal not found"));

        if (!meal.getUserId().equals(userId)) {
            throw new pl.fitapp.backend.exception.ForbiddenException("Access denied");
        }

        meal.setName(request.getName());
        meal.setCalories(request.getCalories());
        meal.setProteins(request.getProteins() != null ? request.getProteins() : BigDecimal.ZERO);
        meal.setFats(request.getFats() != null ? request.getFats() : BigDecimal.ZERO);
        meal.setCarbohydrates(request.getCarbohydrates() != null ? request.getCarbohydrates() : BigDecimal.ZERO);
        meal.setEatenAt(request.getEatenAt());

        Meal savedMeal = mealRepository.save(meal);
        return toResponse(savedMeal);
    }

    @Transactional
    public void deleteMeal(UUID mealId, UUID userId) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new pl.fitapp.backend.exception.NotFoundException("Meal not found"));

        if (!meal.getUserId().equals(userId)) {
            throw new pl.fitapp.backend.exception.ForbiddenException("Access denied");
        }

        mealRepository.delete(meal);
    }

    private MealResponse toResponse(Meal meal) {
        return MealResponse.builder()
                .id(meal.getId())
                .name(meal.getName())
                .calories(meal.getCalories())
                .proteins(meal.getProteins())
                .fats(meal.getFats())
                .carbohydrates(meal.getCarbohydrates())
                .eatenAt(meal.getEatenAt())
                .createdAt(meal.getCreatedAt())
                .build();
    }
}
