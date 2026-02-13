package pl.fitapp.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.fitapp.backend.domain.DataPackVersion;
import pl.fitapp.backend.domain.DishDictionaryEntry;
import pl.fitapp.backend.domain.DishTemplate;
import pl.fitapp.backend.domain.DishTemplateIngredient;
import pl.fitapp.backend.domain.Food;
import pl.fitapp.backend.domain.FoodSource;
import pl.fitapp.backend.dto.DataPackVersionDTO;
import pl.fitapp.backend.dto.DishDictionaryEntryDTO;
import pl.fitapp.backend.dto.DishTemplateDTO;
import pl.fitapp.backend.dto.DishTemplateIngredientDTO;
import pl.fitapp.backend.dto.FoodLocalDTO;
import pl.fitapp.backend.repository.DataPackVersionRepository;
import pl.fitapp.backend.repository.DishDictionaryRepository;
import pl.fitapp.backend.repository.DishTemplateIngredientRepository;
import pl.fitapp.backend.repository.DishTemplateRepository;
import pl.fitapp.backend.repository.FoodRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DataPackService {

    private final DataPackVersionRepository versionRepository;
    private final DishTemplateRepository templateRepository;
    private final DishTemplateIngredientRepository ingredientRepository;
    private final DishDictionaryRepository dictionaryRepository;
    private final FoodRepository foodRepository;

    @Transactional(readOnly = true)
    public List<DataPackVersionDTO> getVersions() {
        return versionRepository.findAll().stream()
                .map(v -> new DataPackVersionDTO(v.getPackName(), v.getVersion(), v.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportDishTemplates(int sinceVersion) {
        DataPackVersion v = versionRepository.findById("dish_templates")
                .orElse(new DataPackVersion("dish_templates", 0, null));

        List<DishTemplateDTO> items = templateRepository.findAll().stream()
                .map(t -> {
                    List<DishTemplateIngredientDTO> ings = ingredientRepository
                            .findByDishTemplate_DishKeyOrderBySortOrderAsc(t.getDishKey())
                            .stream()
                            .map(this::toDto)
                            .toList();

                    String namePl = (t.getNamePl() != null && !t.getNamePl().isBlank())
                            ? t.getNamePl()
                            : t.getNameEn();

                    return new DishTemplateDTO(
                            t.getDishKey(),
                            t.getNameEn(),
                            namePl,
                            t.getCategory(),
                            t.getDefaultServingG(),
                            ings
                    );
                })
                .toList();

        return Map.of(
                "packName", "dish_templates",
                "version", v.getVersion(),
                "items", items
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportDictionary(String lang, int sinceVersion) {
        final String packName = "lang_" + lang;
        DataPackVersion v = versionRepository.findById(packName)
                .orElse(new DataPackVersion(packName, 0, null));

        List<DishDictionaryEntryDTO> items = dictionaryRepository.findByLangOrderByTermAsc(lang).stream()
                .map(this::toDto)
                .toList();

        return Map.of(
                "packName", packName,
                "version", v.getVersion(),
                "items", items
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportMiniUsda(int sinceVersion) {
        DataPackVersion v = versionRepository.findById("mini_usda")
                .orElse(new DataPackVersion("mini_usda", 0, null));

        List<String> foodKeys = ingredientRepository.findDistinctFoodKeys();
        List<String> externalIds = foodKeys.stream()
                .filter(k -> k != null && k.startsWith("usda:"))
                .map(k -> k.substring("usda:".length()))
                .distinct()
                .toList();

        List<FoodLocalDTO> items;
        if (externalIds.isEmpty()) {
            items = List.of();
        } else {
            items = foodRepository.findByExternalIdInAndSource(externalIds, FoodSource.USDA).stream()
                    .filter(f -> f.getExternalId() != null)
                    .sorted(Comparator.comparing(Food::getExternalId))
                    .map(this::toFoodLocalDto)
                    .toList();
        }

        return Map.of(
                "packName", "mini_usda",
                "version", v.getVersion(),
                "items", items
        );
    }

    private DishTemplateIngredientDTO toDto(DishTemplateIngredient i) {
        String namePl = (i.getNamePl() != null && !i.getNamePl().isBlank())
                ? i.getNamePl()
                : i.getNameEn();

        return new DishTemplateIngredientDTO(
                i.getFoodKey(),
                i.getNameEn(),
                namePl,
                i.getDefaultAmountG(),
                i.getProportionPercent(),
                i.getSortOrder()
        );
    }

    private DishDictionaryEntryDTO toDto(DishDictionaryEntry e) {
        DishTemplate t = e.getDishTemplate();
        return new DishDictionaryEntryDTO(e.getTerm(), t.getDishKey(), e.getLang());
    }

    private FoodLocalDTO toFoodLocalDto(Food f) {
        return new FoodLocalDTO(
                "usda:" + f.getExternalId(),
                f.getName(),
                null,
                nvl(f.getCalories()),
                nvl(f.getProtein()),
                nvl(f.getFat()),
                nvl(f.getCarbohydrates()),
                "mini_usda"
        );
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
