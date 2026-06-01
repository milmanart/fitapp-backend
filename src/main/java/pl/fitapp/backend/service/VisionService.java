package pl.fitapp.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.fitapp.backend.dto.TemplateIngredientDTO;
import pl.fitapp.backend.repository.DishDictionaryRepository;
import pl.fitapp.backend.repository.DishTemplateIngredientRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VisionService {

    private static final String PROMPT =
            "Analyze this food image. Respond ONLY with valid JSON (no markdown, no code blocks):\n" +
            "{\"dish\":\"full dish name or null\",\"ingredients\":[\"item1\",\"item2\"]}\n" +
            "Rules: dish = complete dish name (e.g. Spaghetti Bolognese, Caesar Salad) or null. " +
            "ingredients = all visible food items. English only. No extra text.";

    private final CopilotTokenService copilotTokenService;
    private final DishDictionaryRepository dishDictionaryRepo;
    private final DishTemplateIngredientRepository dishTemplateIngredientRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = buildRestTemplate();

    @Value("${github.copilot.api-url:https://api.individual.githubcopilot.com}")
    private String copilotApiUrl;

    @Value("${github.copilot.model:gpt-5-mini}")
    private String copilotModel;

    @Value("${vision.groq.api-key:}")
    private String groqApiKey;

    @Value("${vision.groq.model:meta-llama/llama-4-scout-17b-16e-instruct}")
    private String groqModel;

    @Value("${vision.groq.base-url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqBaseUrl;

    public VisionResult identifyFoods(String imageBase64, String mimeType) {
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            try {
                VisionResult result = callGroq(imageBase64, mimeType);
                if (result != null && !result.ingredients().isEmpty()) {
                    log.info("Vision: Groq dish=[{}] ingredients={}", result.dish(), result.ingredients().size());
                    return result;
                }
            } catch (Exception e) {
                log.warn("Vision: Groq failed: {}", e.getMessage());
            }
        }
        if (copilotTokenService.isConfigured()) {
            try {
                String token = copilotTokenService.getCopilotToken();
                if (token != null) {
                    VisionResult result = callCopilot(token, imageBase64, mimeType);
                    if (result != null && !result.ingredients().isEmpty()) {
                        log.info("Vision: Copilot dish=[{}] ingredients={}", result.dish(), result.ingredients().size());
                        return result;
                    }
                }
            } catch (Exception e) {
                log.warn("Vision: Copilot failed: {}", e.getMessage());
            }
        }
        log.warn("Vision: all providers failed");
        return VisionResult.empty();
    }

    private VisionResult callCopilot(String token, String imageBase64, String mimeType) throws Exception {
        Map<String, Object> body = Map.of(
                "model", copilotModel, "max_tokens", 512,
                "messages", List.of(Map.of("role", "user", "content", List.of(
                        Map.of("type", "image_url", "image_url",
                                Map.of("url", "data:" + mimeType + ";base64," + imageBase64)),
                        Map.of("type", "text", "text", PROMPT)))));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("Copilot-Integration-Id", copilotTokenService.getIntegrationId());
        ResponseEntity<String> response = restTemplate.postForEntity(
                copilotApiUrl + "/chat/completions",
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);
        return parseDishResponse(extractContent(response.getBody()), "copilot");
    }

    private VisionResult callGroq(String imageBase64, String mimeType) throws Exception {
        Map<String, Object> body = Map.of(
                "model", groqModel, "max_tokens", 512, "temperature", 0.1,
                "messages", List.of(Map.of("role", "user", "content", List.of(
                        Map.of("type", "image_url", "image_url",
                                Map.of("url", "data:" + mimeType + ";base64," + imageBase64)),
                        Map.of("type", "text", "text", PROMPT)))));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);
        ResponseEntity<String> response = restTemplate.postForEntity(
                groqBaseUrl,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);
        return parseDishResponse(extractContent(response.getBody()), "groq");
    }

    private String extractContent(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        return root.path("choices").path(0).path("message").path("content").asText("").trim();
    }

    private VisionResult parseDishResponse(String text, String provider) {
        try {
            String clean = text.trim();
            if (clean.startsWith("```")) {
                int nl = clean.indexOf("\n");
                int fence = clean.lastIndexOf("```");
                if (nl > 0 && fence > nl) clean = clean.substring(nl + 1, fence).trim();
            }
            int start = clean.indexOf("{");
            int end = clean.lastIndexOf("}");
            if (start >= 0 && end > start) clean = clean.substring(start, end + 1);
            JsonNode node = objectMapper.readTree(clean);
            String dish = null;
            if (node.has("dish") && !node.get("dish").isNull()) {
                String d = node.get("dish").asText("").trim();
                if (!d.isEmpty() && !d.equalsIgnoreCase("null")) dish = d;
            }
            List<String> ingredients = new ArrayList<>();
            if (node.has("ingredients")) {
                node.get("ingredients").forEach(n -> {
                    String v = n.asText("").trim();
                    if (!v.isEmpty()) ingredients.add(v);
                });
            }
            if (dish != null) {
                String dishKey = findDishKey(dish);
                if (dishKey != null) {
                    List<TemplateIngredientDTO> tpl = loadTemplateIngredients(dishKey);
                    log.info("Vision: matched [{}] -> key [{}] ({} ingredients)", dish, dishKey, tpl.size());
                    return new VisionResult(dish, dishKey, tpl, ingredients, provider);
                }
                log.info("Vision: dish [{}] not found in dictionary", dish);
            }
            return VisionResult.ingredientsOnly(ingredients, provider);
        } catch (Exception e) {
            log.warn("Vision: parse failed [{}]: {}", provider, e.getMessage());
            return VisionResult.empty();
        }
    }

    private String findDishKey(String dishName) {
        var exact = dishDictionaryRepo.findFirstByTermIgnoreCaseAndLang(dishName, "en");
        if (exact.isPresent()) return exact.get().getDishTemplate().getDishKey();
        var contains = dishDictionaryRepo.findByTermContainingIgnoreCaseAndLang(dishName, "en");
        if (!contains.isEmpty()) return contains.get(0).getDishTemplate().getDishKey();
        for (String word : dishName.split("\\s+")) {
            if (word.length() < 4) continue;
            var wm = dishDictionaryRepo.findByTermContainingIgnoreCaseAndLang(word, "en");
            if (!wm.isEmpty()) return wm.get(0).getDishTemplate().getDishKey();
        }
        return null;
    }

    private List<TemplateIngredientDTO> loadTemplateIngredients(String dishKey) {
        return dishTemplateIngredientRepo.findByDishTemplate_DishKeyOrderBySortOrderAsc(dishKey)
                .stream()
                .map(i -> new TemplateIngredientDTO(
                        i.getFoodKey(), i.getNameEn(), i.getNamePl(),
                        i.getDefaultAmountG().doubleValue()))
                .toList();
    }

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        return new RestTemplate(factory);
    }
}
