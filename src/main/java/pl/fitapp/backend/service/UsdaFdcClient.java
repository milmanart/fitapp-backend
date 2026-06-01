package pl.fitapp.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import pl.fitapp.backend.domain.Food;
import pl.fitapp.backend.domain.FoodSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class UsdaFdcClient {

    private static final String USER_AGENT = "FitApp/1.0 (fitapp-backend)";

    private final RestTemplate restTemplate;

    @Value("${usda.fdc.api-key:DEMO_KEY}")
    private String apiKey;

    @Value("${usda.fdc.base-url:https://api.nal.usda.gov/fdc/v1/foods/search}")
    private String baseUrl;

    @Value("${usda.fdc.enabled:true}")
    private boolean enabled;

    public UsdaFdcClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @SuppressWarnings("unchecked")
    public List<Food> searchByName(String query, int limit) {
        return searchByName(query, limit, 1);
    }

    @SuppressWarnings("unchecked")
    public List<Food> searchByName(String query, int limit, int maxPages) {
        if (!enabled) {
            return List.of();
        }

        if (query == null || query.isBlank()) {
            return List.of();
        }

        final int pageSize = Math.max(1, Math.min(limit, 100));
        final int pagesToFetch = Math.max(1, Math.min(maxPages, 8));

        List<Food> out = new ArrayList<>();
        Set<String> seenExternalIds = new HashSet<>();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            int totalPages = pagesToFetch;
            for (int page = 1; page <= pagesToFetch; page++) {
                String url = String.format(
                        "%s?api_key=%s&query=%s&pageSize=%d&pageNumber=%d",
                        baseUrl,
                        UriUtils.encodeQueryParam(apiKey, StandardCharsets.UTF_8),
                        UriUtils.encodeQueryParam(query, StandardCharsets.UTF_8),
                        pageSize,
                        page
                );

                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
                Map body = response.getBody();
                if (body == null) {
                    break;
                }

                Object totalPagesRaw = body.get("totalPages");
                if (totalPagesRaw instanceof Number number) {
                    totalPages = number.intValue();
                }

                Object foodsRaw = body.get("foods");
                if (!(foodsRaw instanceof List<?> rawItems) || rawItems.isEmpty()) {
                    break;
                }

                for (Object item : rawItems) {
                    if (!(item instanceof Map<?, ?> anyMap)) {
                        continue;
                    }

                    Map<String, Object> raw = (Map<String, Object>) anyMap;
                    Food mapped = mapToFood(raw);
                    if (mapped == null || mapped.getExternalId() == null || mapped.getExternalId().isBlank()) {
                        continue;
                    }

                    if (seenExternalIds.add(mapped.getExternalId())) {
                        out.add(mapped);
                    }
                }

                if (page >= totalPages) {
                    break;
                }
            }

            return out;
        } catch (Exception e) {
            log.warn("[USDA] Search failed for query '{}': {}", query, e.getMessage());
            return out;
        }
    }

    @SuppressWarnings("unchecked")
    private Food mapToFood(Map<String, Object> raw) {
        String fdcId = getString(raw, "fdcId");
        String name = getString(raw, "description");
        if (fdcId == null || fdcId.isBlank() || name == null || name.isBlank()) {
            return null;
        }

        List<Map<String, Object>> nutrients = List.of();
        Object nutrientsRaw = raw.get("foodNutrients");
        if (nutrientsRaw instanceof List<?> list) {
            nutrients = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    nutrients.add((Map<String, Object>) map);
                }
            }
        }

        Food food = new Food();
        food.setSource(FoodSource.USDA);
        food.setExternalId(fdcId);
        food.setName(name);
        food.setBrand(firstNonBlank(getString(raw, "brandOwner"), getString(raw, "brandName")));

        food.setCalories(findNutrient(nutrients, "1008", "energy"));
        food.setProtein(findNutrient(nutrients, "1003", "protein"));
        food.setFat(findNutrient(nutrients, "1004", "total lipid", "fat"));
        food.setCarbohydrates(findNutrient(nutrients, "1005", "carbohydrate"));
        food.setFiber(findNutrient(nutrients, "1079", "fiber"));
        food.setSugar(findNutrient(nutrients, "2000", "sugar"));
        food.setSodium(findNutrient(nutrients, "1093", "sodium"));

        BigDecimal servingSize = getDecimal(raw, "servingSize");
        if (servingSize.compareTo(BigDecimal.ZERO) > 0) {
            food.setServingSize(servingSize);
            food.setServingUnit(getString(raw, "servingSizeUnit"));
        }

        food.setLanguage("en");
        food.setCountryCodes("US");
        return food;
    }

    private BigDecimal findNutrient(List<Map<String, Object>> nutrients,
                                    String nutrientNumber,
                                    String... nameHints) {
        if (nutrients == null || nutrients.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Set<String> hints = new HashSet<>();
        for (String hint : nameHints) {
            hints.add(hint.toLowerCase(Locale.ROOT));
        }

        for (Map<String, Object> nutrient : nutrients) {
            String number = getString(nutrient, "nutrientNumber");
            String name = getString(nutrient, "nutrientName");

            if (nutrientNumber.equals(number)) {
                return getDecimal(nutrient, "value");
            }

            if (name != null) {
                String normalized = name.toLowerCase(Locale.ROOT);
                for (String hint : hints) {
                    if (normalized.contains(hint)) {
                        return getDecimal(nutrient, "value");
                    }
                }
            }
        }

        return BigDecimal.ZERO;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return null;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private BigDecimal getDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
