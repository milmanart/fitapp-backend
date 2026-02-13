package pl.fitapp.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.fitapp.backend.dto.DailyNutritionResponse;
import pl.fitapp.backend.dto.MealRequest;
import pl.fitapp.backend.dto.MealResponse;
import pl.fitapp.backend.service.MealService;
import pl.fitapp.backend.service.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<MealResponse>> getMeals(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID userId = getUserId(userDetails);
        
        List<MealResponse> meals;
        if (date != null) {
            meals = mealService.getMealsByUserIdAndDate(userId, date);
        } else {
            meals = mealService.getMealsByUserId(userId);
        }
        return ResponseEntity.ok(meals);
    }

    @GetMapping("/nutrition")
    public ResponseEntity<DailyNutritionResponse> getDailyNutrition(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID userId = getUserId(userDetails);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        
        DailyNutritionResponse nutrition = mealService.getDailyNutrition(userId, targetDate);
        return ResponseEntity.ok(nutrition);
    }

    @PostMapping
    public ResponseEntity<MealResponse> createMeal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MealRequest request) {
        UUID userId = getUserId(userDetails);
        MealResponse response = mealService.createMeal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MealResponse> updateMeal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody MealRequest request) {
        UUID userId = getUserId(userDetails);
        MealResponse response = mealService.updateMeal(id, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = getUserId(userDetails);
        mealService.deleteMeal(id, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getUserId(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername()).getId();
    }
}
