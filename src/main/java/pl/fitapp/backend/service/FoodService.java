package pl.fitapp.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.fitapp.backend.domain.Food;
import pl.fitapp.backend.domain.FoodSource;
import pl.fitapp.backend.repository.FoodRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FoodService {

    private final FoodRepository foodRepository;

    public List<Food> searchFoods(String query, String locale) {
        log.info("Searching foods: query='{}', locale='{}'", query, locale);

        List<Food> usdaResults = foodRepository.findByNameContainingIgnoreCaseAndSource(
                query,
                FoodSource.USDA
        );

        log.info("Found {} USDA results for query: {}", usdaResults.size(), query);

        if (usdaResults.size() > 20) {
            return usdaResults.subList(0, 20);
        }

        return usdaResults;
    }

    public List<Food> searchFoodsFull(String query, int limit) {
        log.info("Full-text search: query='{}', limit={}", query, limit);

        List<Food> results = foodRepository.searchByNameFullText(query, limit);

        log.info("Found {} results", results.size());

        return results;
    }

    public Optional<Food> getFoodById(String id) {
        try {
            return foodRepository.findById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", id);
            return Optional.empty();
        }
    }

    public Optional<Food> getFoodByExternalId(String externalId, FoodSource source) {
        return foodRepository.findByExternalIdAndSource(externalId, source);
    }

    public Optional<Food> resolveFoodKey(String foodKey) {
        if (foodKey == null) return Optional.empty();
        final String key = foodKey.trim();
        if (key.isEmpty()) return Optional.empty();

        if (key.startsWith("usda:")) {
            return getFoodByExternalId(key.substring("usda:".length()), FoodSource.USDA);
        }

        if (key.startsWith("backend:")) {
            return getFoodById(key.substring("backend:".length()));
        }

        if (key.matches("\\d+")) {
            return getFoodByExternalId(key, FoodSource.USDA);
        }

        return getFoodById(key);
    }

    public List<Food> resolveFoodKeys(List<String> foodKeys) {
        if (foodKeys == null || foodKeys.isEmpty()) return List.of();
        final List<Food> out = new ArrayList<>();
        for (final String k : foodKeys) {
            resolveFoodKey(k).ifPresent(out::add);
        }
        return out;
    }

    public Optional<Food> findByBarcode(String barcode) {
        log.info("Searching by barcode: {}", barcode);

        Optional<Food> result = foodRepository.findByBarcode(barcode);

        if (result.isEmpty()) {
            log.info("No food found for barcode: {}", barcode);
        }

        return result;
    }

    public List<Food> getFoodsBySource(FoodSource source) {
        return foodRepository.findBySource(source);
    }

    public List<Food> getAllFoodsBySource(FoodSource source) {
        return foodRepository.findBySource(source);
    }

    public long getTotalCount() {
        return foodRepository.count();
    }

    public long getCountBySource(FoodSource source) {
        return foodRepository.countBySource(source);
    }
}
