package pl.fitapp.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.fitapp.backend.domain.Food;
import pl.fitapp.backend.domain.FoodSource;
import pl.fitapp.backend.dto.FoodDTO;
import pl.fitapp.backend.dto.FoodResolveBatchRequest;
import pl.fitapp.backend.service.FoodService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
@Slf4j
public class FoodController {

    private final FoodService foodService;

    @GetMapping("/search")
    public ResponseEntity<List<FoodDTO>> searchFoods(
            @RequestParam String query,
            @RequestParam(defaultValue = "en") String locale,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("Search request: query='{}', locale='{}', limit={}", query, locale, limit);

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Food> foods = foodService.searchFoods(query.trim(), locale);

        List<FoodDTO> dtos = foods.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/resolve/{foodKey:.+}")
    public ResponseEntity<FoodDTO> resolve(@PathVariable String foodKey) {
        return foodService.resolveFoodKey(foodKey)
                .map(food -> ResponseEntity.ok(toDTO(food)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/resolve-batch")
    public ResponseEntity<List<FoodDTO>> resolveBatch(@RequestBody FoodResolveBatchRequest request) {
        final List<String> keys = request != null ? request.getFoodKeys() : null;
        final List<FoodDTO> dtos = foodService.resolveFoodKeys(keys).stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/search-full")
    public ResponseEntity<List<FoodDTO>> searchFoodsFull(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Food> foods = foodService.searchFoodsFull(query.trim(), limit);

        List<FoodDTO> dtos = foods.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodDTO> getFoodById(@PathVariable String id) {
        return foodService.getFoodById(id)
                .map(food -> ResponseEntity.ok(toDTO(food)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<FoodDTO> getFoodByBarcode(@PathVariable String barcode) {
        log.info("Barcode search: {}", barcode);

        return foodService.findByBarcode(barcode)
                .map(food -> ResponseEntity.ok(toDTO(food)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalCount = foodService.getTotalCount();
        long usdaCount = foodService.getCountBySource(FoodSource.USDA);

        Map<String, Object> stats = Map.of(
                "total", totalCount,
                "usda", usdaCount,
                "openfoodfacts", 0,
                "manual", 0
        );

        return ResponseEntity.ok(stats);
    }

    private FoodDTO toDTO(Food food) {
        FoodDTO dto = new FoodDTO();
        dto.setId(food.getId() != null ? food.getId().toString() : null);
        dto.setExternalId(food.getExternalId());
        dto.setSource(food.getSource() != null ? food.getSource().name() : null);
        dto.setBarcode(food.getBarcode());
        dto.setName(food.getName());
        dto.setBrand(food.getBrand());
        dto.setServingSize(food.getServingSize());
        dto.setServingUnit(food.getServingUnit());
        dto.setCalories(food.getCalories());
        dto.setProtein(food.getProtein());
        dto.setFat(food.getFat());
        dto.setCarbohydrates(food.getCarbohydrates());
        dto.setFiber(food.getFiber());
        dto.setSugar(food.getSugar());
        dto.setSodium(food.getSodium());
        dto.setLanguage(food.getLanguage());
        dto.setCountryCodes(food.getCountryCodes());
        dto.setImageUrl(food.getImageUrl());
        return dto;
    }
}
