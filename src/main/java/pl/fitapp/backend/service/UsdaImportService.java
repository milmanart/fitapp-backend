package pl.fitapp.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.fitapp.backend.domain.Food;
import pl.fitapp.backend.domain.FoodSource;
import pl.fitapp.backend.repository.FoodRepository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UsdaImportService {

    private final FoodRepository foodRepository;

    @Transactional
    public void importFromCsv(String basePath) throws Exception {
        log.info("Starting USDA import from: {}", basePath);

        Map<String, FoodData> foods = loadFoods(basePath + "/food.csv");
        log.info("Loaded {} foods", foods.size());

        Map<String, NutritionData> nutrients = loadNutrients(basePath + "/food_nutrient.csv", foods.keySet());
        log.info("Loaded nutrition for {} foods", nutrients.size());

        List<Food> foodEntities = new ArrayList<>();
        int skipped = 0;

        for (Map.Entry<String, FoodData> entry : foods.entrySet()) {
            String fdcId = entry.getKey();
            FoodData foodData = entry.getValue();
            NutritionData nutr = nutrients.get(fdcId);

            if (nutr == null || !nutr.isComplete()) {
                skipped++;
                continue;
            }

            Food food = new Food();
            food.setExternalId(fdcId);
            food.setSource(FoodSource.USDA);
            food.setName(foodData.name);
            food.setCalories(nutr.calories);
            food.setProtein(nutr.protein);
            food.setFat(nutr.fat);
            food.setCarbohydrates(nutr.carbs);
            food.setFiber(nutr.fiber);
            food.setSugar(nutr.sugar);
            food.setLanguage("en");
            food.setCountryCodes("US");

            foodEntities.add(food);

            if (foodEntities.size() >= 1000) {
                foodRepository.saveAll(foodEntities);
                log.info("Saved {} foods...", foodEntities.size());
                foodEntities.clear();
            }
        }

        if (!foodEntities.isEmpty()) {
            foodRepository.saveAll(foodEntities);
        }

        log.info("Import complete! Imported {} foods, skipped {}", foods.size() - skipped, skipped);
    }

    private Map<String, FoodData> loadFoods(String filePath) throws Exception {
        Map<String, FoodData> foods = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();

            while ((line = br.readLine()) != null) {
                String[] parts = parseCsvLine(line);
                if (parts.length >= 3 && "sr_legacy_food".equals(parts[1])) {
                    foods.put(parts[0], new FoodData(parts[2]));
                }
            }
        }

        return foods;
    }

    private Map<String, NutritionData> loadNutrients(String filePath, Set<String> validFdcIds) throws Exception {
        Map<String, NutritionData> nutrients = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();

            while ((line = br.readLine()) != null) {
                String[] parts = parseCsvLine(line);
                if (parts.length >= 4) {
                    String fdcId = parts[1];
                    if (!validFdcIds.contains(fdcId)) continue;

                    String nutrientId = parts[2];
                    String amount = parts[3];

                    if (amount == null || amount.isEmpty()) continue;

                    nutrients.putIfAbsent(fdcId, new NutritionData());
                    NutritionData nd = nutrients.get(fdcId);

                    try {
                        BigDecimal value = new BigDecimal(amount);
                        switch (nutrientId) {
                            case "1008": nd.calories = value; break;
                            case "1003": nd.protein = value; break;
                            case "1004": nd.fat = value; break;
                            case "1005": nd.carbs = value; break;
                            case "2033": nd.fiber = value; break;
                            case "2000": nd.sugar = value; break;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return nutrients;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    static class FoodData {
        String name;
        FoodData(String name) { this.name = name; }
    }

    static class NutritionData {
        BigDecimal calories, protein, fat, carbs, fiber, sugar;

        boolean isComplete() {
            return calories != null && protein != null && fat != null && carbs != null;
        }
    }
}
