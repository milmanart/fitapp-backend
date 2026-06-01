package pl.fitapp.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import pl.fitapp.backend.domain.Food;
import pl.fitapp.backend.domain.FoodSource;

import java.math.BigDecimal;
import java.util.*;

@Component
@Slf4j
public class OpenFoodFactsClient {

    private static final String BARCODE_URL = "https://world.openfoodfacts.net/api/v2/product/{barcode}?fields=code,product_name,brands,nutriments,image_url,quantity,product_quantity,product_quantity_unit,serving_quantity,serving_quantity_unit,categories_tags";
    private static final String SEARCH_URL = "https://world.openfoodfacts.org/cgi/search.pl?search_terms={query}&json=1&page_size={limit}&fields=code,product_name,brands,nutriments,image_url,quantity,product_quantity,product_quantity_unit,serving_quantity,serving_quantity_unit,categories_tags";
    private static final String USER_AGENT = "FitApp/1.0 (fitapp-backend)";

    private final RestTemplate restTemplate;

    public OpenFoodFactsClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public Optional<Food> fetchByBarcode(String barcode) {
        try {
            log.info("[OFF] Fetching barcode: {}", barcode);
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    BARCODE_URL.replace("{barcode}", barcode),
                    HttpMethod.GET, entity, Map.class);

            Map body = response.getBody();
            if (body == null) return Optional.empty();

            Integer status = (Integer) body.get("status");
            if (status == null || status != 1) {
                log.info("[OFF] Product not found for barcode: {}", barcode);
                return Optional.empty();
            }

            Map product = (Map) body.get("product");
            if (product == null) return Optional.empty();

            return Optional.ofNullable(mapToFood(product, barcode));
        } catch (Exception e) {
            log.error("[OFF] Error fetching barcode {}: {}", barcode, e.getMessage());
            return Optional.empty();
        }
    }

    public List<Food> searchByName(String query, int limit) {
        try {
            log.info("[OFF] Searching: query='{}', limit={}", query, limit);
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = SEARCH_URL
                    .replace("{query}", query)
                    .replace("{limit}", String.valueOf(limit));

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            Map body = response.getBody();
            if (body == null) return List.of();

            List<Map> products = (List<Map>) body.get("products");
            if (products == null || products.isEmpty()) return List.of();

            List<Food> result = new ArrayList<>();
            for (Map product : products) {
                String code = getString(product, "code");
                Food food = mapToFood(product, code);
                if (food != null) result.add(food);
            }

            log.info("[OFF] Found {} results for query: {}", result.size(), query);
            return result;
        } catch (Exception e) {
            log.error("[OFF] Error searching '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    private Food mapToFood(Map product, String barcode) {
        String name = getString(product, "product_name");
        if (name == null || name.isBlank()) return null;

        Map nutriments = (Map) product.get("nutriments");
        if (nutriments == null) return null;

        Food food = new Food();
        food.setSource(FoodSource.OPENFOODFACTS);
        food.setExternalId(barcode);
        food.setBarcode(barcode);
        food.setName(name);
        food.setBrand(getString(product, "brands"));
        food.setImageUrl(getString(product, "image_url"));
        food.setCalories(getDecimal(nutriments, "energy-kcal_100g"));
        food.setProtein(getDecimal(nutriments, "proteins_100g"));
        food.setFat(getDecimal(nutriments, "fat_100g"));
        food.setCarbohydrates(getDecimal(nutriments, "carbohydrates_100g"));
        food.setFiber(getDecimal(nutriments, "fiber_100g"));
        food.setSugar(getDecimal(nutriments, "sugars_100g"));
        food.setSodium(getDecimal(nutriments, "sodium_100g"));
        BigDecimal packageSize = getDecimal(product, "product_quantity");
        if (packageSize.compareTo(BigDecimal.ZERO) <= 0) {
            packageSize = parseQuantityValue(getString(product, "quantity"));
        }
        if (packageSize.compareTo(BigDecimal.ZERO) <= 0) {
            packageSize = getDecimal(product, "serving_quantity");
        }
        food.setServingSize(packageSize);

        String packageUnit = getString(product, "product_quantity_unit");
        if (packageUnit == null || packageUnit.isBlank()) {
            packageUnit = parseQuantityUnit(getString(product, "quantity"));
        }
        if (packageUnit == null || packageUnit.isBlank()) {
            packageUnit = getString(product, "serving_quantity_unit");
        }
        if (packageUnit != null && !packageUnit.isBlank()) {
            food.setServingUnit(packageUnit);
        }

        food.setLanguage("en");
        return food;
    }

    private BigDecimal parseQuantityValue(String quantityText) {
        if (quantityText == null || quantityText.isBlank()) return BigDecimal.ZERO;
        String normalized = quantityText.toLowerCase(Locale.ROOT).replace(',', '.');

        try {
            java.util.regex.Matcher multiplied = java.util.regex.Pattern
                    .compile("(\\d+(?:\\.\\d+)?)\\s*(?:pcs?|pieces?|szt\\.?|pack)?\\s*[x×*]\\s*(\\d+(?:\\.\\d+)?)")
                    .matcher(normalized);
            if (multiplied.find()) {
                BigDecimal left = new BigDecimal(multiplied.group(1));
                BigDecimal right = new BigDecimal(multiplied.group(2));
                return left.multiply(right);
            }

            java.util.regex.Matcher single = java.util.regex.Pattern
                    .compile("(\\d+(?:\\.\\d+)?)")
                    .matcher(normalized);
            if (single.find()) {
                return new BigDecimal(single.group(1));
            }
        } catch (Exception ignored) {

        }

        return BigDecimal.ZERO;
    }

    private String parseQuantityUnit(String quantityText) {
        if (quantityText == null || quantityText.isBlank()) return null;
        String normalized = quantityText.toLowerCase(Locale.ROOT).replace(',', '.');

        try {
            java.util.regex.Matcher multiplied = java.util.regex.Pattern
                    .compile("(?:\\d+(?:\\.\\d+)?)\\s*(?:pcs?|pieces?|szt\\.?|pack)?\\s*[x×*]\\s*(?:\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)")
                    .matcher(normalized);
            if (multiplied.find()) {
                return multiplied.group(1);
            }

            java.util.regex.Matcher single = java.util.regex.Pattern
                    .compile("(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)")
                    .matcher(normalized);
            if (single.find()) {
                return single.group(2);
            }
        } catch (Exception ignored) {

        }

        return null;
    }

    private String getString(Map map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private BigDecimal getDecimal(Map map, String key) {
        Object val = map.get(key);
        if (val == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
