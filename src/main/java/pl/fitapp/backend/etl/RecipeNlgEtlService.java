package pl.fitapp.backend.etl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeNlgEtlService {

    private static final int MAX_RECIPES_PER_DISH = 200;
    private static final int MIN_INGREDIENTS = 3;
    private static final int MAX_INGREDIENTS_CAP = 8;

    private final JdbcTemplate jdbc;

    private static final String[] CSV_HEADERS = {"idx", "title", "ingredients", "directions", "link", "source", "ner"};

    private static final Set<String> TITLE_NOISE_WORDS = Set.of(
            "best", "easy", "quick", "ultimate", "simple", "amazing", "homemade", "mom", "grandma", "copycat"
    );

    private static final Set<String> ING_UNITS = Set.of(
            "cup", "cups", "c", "tbsp", "tablespoon", "tablespoons", "tsp", "teaspoon", "teaspoons",
            "oz", "ounce", "ounces", "lb", "lbs", "pound", "pounds",
            "g", "gram", "grams", "kg", "kilo", "kilos", "ml", "milliliter", "milliliters", "l", "liter", "liters",
            "pinch", "dash",
            "can", "cans", "jar", "jars", "package", "packages", "pkg", "pkgs",
            "clove", "cloves", "slice", "slices", "piece", "pieces", "stick", "sticks"
    );

    private static final Set<String> ING_PREP = Set.of(
            "chopped", "diced", "minced", "fresh", "grated", "melted", "cooked", "drained", "sliced", "shredded",
            "crushed", "peeled", "seeded", "rinsed", "softened", "thawed", "frozen", "dried", "canned",
            "boned", "boneless", "skinless", "trimmed", "lean",
            "cut", "up",
            "firmly", "packed", "bite", "size", "broken",
            "small", "medium", "large",
            "optional", "divided", "plus", "more", "taste", "needed", "about", "approx", "approximately",
            "of", "and", "or", "with", "to"
    );

    private static final Set<String> GENERIC_INGS = Set.of("water", "salt", "pepper");

    private static final Set<String> MATCH_STOPWORDS = Set.of(
            "of", "and", "or", "with", "to", "a", "an", "the"
    );

    private CSVFormat csvFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader(CSV_HEADERS)
                .setSkipHeaderRecord(true)
                .build();
    }

    @Transactional
    public void run(
            String csvPath,
            int topDishes,
            int minRecipesPerDish,
            double minIngredientSupport,
            int maxIngredients,
            int defaultServingG,
            boolean truncate,
            boolean dryRun
    ) throws Exception {
        Path path = Path.of(csvPath).toAbsolutePath();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("RecipeNLG CSV not found: " + path);
        }

        int effectiveMaxIngredients = Math.min(Math.max(1, maxIngredients), MAX_INGREDIENTS_CAP);

        log.info("[ETL] RecipeNLG csvPath={}", path);
        log.info("[ETL] Params: topDishes={}, minRecipesPerDish={}, minIngredientSupport={}, maxIngredients(effective)={}, defaultServingG={}, truncate={}, dryRun={}",
                topDishes, minRecipesPerDish, minIngredientSupport, effectiveMaxIngredients, defaultServingG, truncate, dryRun);

        Metrics metrics = new Metrics();

        Map<String, Integer> titleCounts = new HashMap<>(200_000);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = csvFormat().parse(reader)) {
            for (CSVRecord r : parser) {
                metrics.totalRows++;
                String cleaned = cleanTitle(r.get("title"), metrics);
                if (cleaned == null) continue;
                titleCounts.merge(cleaned, 1, Integer::sum);
            }
        }

        log.info("[ETL] Pass1 done: totalRows={}, keptTitles={}, uniqueCleanedTitles={}",
                metrics.totalRows, metrics.titlesKept, titleCounts.size());
        log.info("[ETL] Title rejects: nullOrBlank={}, prefixFiltered={}, lengthFiltered={}, tokenFilteredToEmpty={}",
                metrics.titleRejectNullOrBlank, metrics.titleRejectPrefix, metrics.titleRejectLength, metrics.titleRejectBecameEmpty);

        List<Map.Entry<String, Integer>> selectedTitles = titleCounts.entrySet().stream()
                .filter(e -> e.getValue() >= Math.max(1, minRecipesPerDish))
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(topDishes)
                .toList();

        if (selectedTitles.isEmpty()) {
            log.warn("[ETL] No titles matched selection (minRecipesPerDish={}, topDishes={}).", minRecipesPerDish, topDishes);
            return;
        }

        Map<String, DishAgg> dishes = new HashMap<>(selectedTitles.size() * 2);
        for (Map.Entry<String, Integer> e : selectedTitles) {
            dishes.put(e.getKey(), new DishAgg(e.getKey(), e.getValue()));
        }

        log.info("[ETL] Selected dishes (candidates)={}", dishes.size());

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = csvFormat().parse(reader)) {
            for (CSVRecord r : parser) {
                String cleanedTitle = cleanTitle(r.get("title"), null);
                if (cleanedTitle == null) continue;

                DishAgg agg = dishes.get(cleanedTitle);
                if (agg == null) continue;
                if (agg.recipesConsidered >= MAX_RECIPES_PER_DISH) continue;

                agg.recipesConsidered++;

                List<String> ingLines = parsePythonStringList(r.get("ingredients"));
                if (ingLines.isEmpty()) {
                    ingLines = parsePythonStringList(r.get("ner"));
                }

                for (String line : ingLines) {
                    metrics.ingredientLinesTotal++;
                    String token = normalizeIngredientLine(line);
                    if (token.isEmpty()) {
                        metrics.ingredientTokenEmpty++;
                        continue;
                    }
                    agg.ingredientCounts.merge(token, 1, Integer::sum);
                }
            }
        }

        log.info("[ETL] Pass2 done: ingredientLinesTotal={}, ingredientTokenEmpty={}",
                metrics.ingredientLinesTotal, metrics.ingredientTokenEmpty);

        Map<String, UsdaMatch> ingredientCache = new HashMap<>(200_000);

        List<DishRow> dishRows = new ArrayList<>(dishes.size());
        List<IngredientRow> ingredientRows = new ArrayList<>(dishes.size() * effectiveMaxIngredients);
        List<DictionaryRow> dictionaryRows = new ArrayList<>(dishes.size());

        Set<String> usedDishKeys = new HashSet<>(dishes.size() * 2);

        for (DishAgg agg : dishes.values()) {
            if (agg.recipesConsidered == 0) {
                metrics.dishRejectNoRecipes++;
                continue;
            }

            int minFloor = Math.max(1, (int) Math.ceil(minIngredientSupport * agg.recipesConsidered));

            List<Map.Entry<String, Integer>> sortedTokens = agg.ingredientCounts.entrySet().stream()
                    .filter(e -> e.getValue() >= minFloor)
                    .sorted((a, b) -> {
                        int cmp = Integer.compare(b.getValue(), a.getValue());
                        if (cmp != 0) return cmp;
                        cmp = Integer.compare(ingredientKindScore(b.getKey()), ingredientKindScore(a.getKey()));
                        if (cmp != 0) return cmp;
                        cmp = Integer.compare(b.getKey().length(), a.getKey().length());
                        if (cmp != 0) return cmp;
                        return a.getKey().compareTo(b.getKey());
                    })
                    .toList();

            if (sortedTokens.isEmpty()) {
                metrics.dishRejectNoIngredientsAfterSupport++;
                continue;
            }

            List<String> primary = new ArrayList<>(effectiveMaxIngredients);
            List<String> generic = new ArrayList<>(effectiveMaxIngredients);
            for (Map.Entry<String, Integer> e : sortedTokens) {
                String t = e.getKey();
                if (isGenericIngredient(t)) generic.add(t);
                else primary.add(t);
                if (primary.size() + generic.size() >= Math.max(50, effectiveMaxIngredients * 10)) break;
            }

            List<String> selectedTokens = new ArrayList<>(effectiveMaxIngredients);
            for (String t : primary) {
                selectedTokens.add(t);
                if (selectedTokens.size() >= effectiveMaxIngredients) break;
            }

            if (selectedTokens.size() < MIN_INGREDIENTS) {
                for (String t : generic) {
                    selectedTokens.add(t);
                    if (selectedTokens.size() >= effectiveMaxIngredients) break;
                }
            }

            if (selectedTokens.size() < MIN_INGREDIENTS) {
                metrics.dishRejectTooFewIngredients++;
                continue;
            }

            List<MappedIngredient> mapped = new ArrayList<>(selectedTokens.size());
            for (String token : selectedTokens) {
                UsdaMatch match = ingredientCache.get(token);
                if (match == null && !ingredientCache.containsKey(token)) {
                    match = matchUsda(token, metrics);
                    ingredientCache.put(token, match);
                }
                if (match == null) {
                    metrics.ingredientUnmapped++;
                    continue;
                }
                mapped.add(new MappedIngredient(token, match.foodKey, match.canonicalName));
            }

            if (mapped.size() < MIN_INGREDIENTS) {
                metrics.dishRejectTooFewMappedIngredients++;
                continue;
            }

            String dishKey = generateDishKey(agg.cleanedTitle, usedDishKeys);
            String nameEn = titleCase(agg.cleanedTitle);

            dishRows.add(new DishRow(dishKey, nameEn, null, null, BigDecimal.valueOf(defaultServingG)));
            dictionaryRows.add(new DictionaryRow(agg.cleanedTitle, dishKey, "en"));

            BigDecimal totalG = BigDecimal.ZERO;
            List<BigDecimal> gramsList = new ArrayList<>(mapped.size());
            for (MappedIngredient mi : mapped) {
                BigDecimal grams = BigDecimal.valueOf(defaultGramsForIngredient(mi.token));
                gramsList.add(grams);
                totalG = totalG.add(grams);
            }

            int sortOrder = 1;
            for (int i = 0; i < mapped.size(); i++) {
                MappedIngredient mi = mapped.get(i);
                BigDecimal grams = gramsList.get(i).setScale(2, RoundingMode.HALF_UP);
                BigDecimal proportion = null;
                if (totalG.signum() > 0) {
                    proportion = grams.divide(totalG, 4, RoundingMode.HALF_UP);
                }
                ingredientRows.add(new IngredientRow(
                        dishKey,
                        mi.foodKey,
                        mi.canonicalName,
                        null,
                        grams,
                        proportion,
                        sortOrder++
                ));
            }

            metrics.dishesKept++;
            metrics.ingredientsKept += mapped.size();

            if (metrics.exampleDishes.size() < 20) {
                metrics.exampleDishes.add(new ExampleDish(dishKey, agg.cleanedTitle, mapped));
            }
        }

        log.info("[ETL] Prepared: dishesKept={}, dishTemplatesRows={}, ingredientsRows={}, dictionaryRows(en)={}",
                metrics.dishesKept, dishRows.size(), ingredientRows.size(), dictionaryRows.size());

        if (metrics.dishesKept > 0) {
            double avgIng = (double) metrics.ingredientsKept / (double) metrics.dishesKept;
            log.info("[ETL] Avg ingredients per dish={}", String.format(Locale.ROOT, "%.2f", avgIng));
        }

        long dishRejectsTotal = metrics.dishRejectNoRecipes
                + metrics.dishRejectNoIngredientsAfterSupport
                + metrics.dishRejectTooFewIngredients
                + metrics.dishRejectTooFewMappedIngredients;
        log.info("[ETL] Dish rejects: noRecipes={}, noIngredientsAfterSupport={}, tooFewIngredients={}, tooFewMappedIngredients={}, totalRejects={}",
                metrics.dishRejectNoRecipes,
                metrics.dishRejectNoIngredientsAfterSupport,
                metrics.dishRejectTooFewIngredients,
                metrics.dishRejectTooFewMappedIngredients,
                dishRejectsTotal);

        log.info("[ETL] Ingredient mapping: attemptedTokens={}, mappedTokens={}, unmappedTokens={}",
                metrics.ingredientMapAttempts, metrics.ingredientMapped, metrics.ingredientUnmapped);

        if (!metrics.exampleDishes.isEmpty()) {
            for (ExampleDish ex : metrics.exampleDishes) {
                String ingredients = ex.mapped.stream()
                        .map(mi -> mi.token + " -> " + mi.canonicalName + " (" + defaultGramsForIngredient(mi.token) + "g)")
                        .limit(12)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                log.info("[ETL] Example: term='{}' dish_key='{}' ingredients=[{}]", ex.term, ex.dishKey, ingredients);
            }
        }

        if (dryRun) {
            log.info("[ETL] Dry-run. No DB writes.");
            return;
        }

        if (truncate) {
            log.info("[ETL] Truncating dish_templates (CASCADE)...");
            jdbc.execute("TRUNCATE dish_templates CASCADE");
        } else {
            if (!dishRows.isEmpty()) {
                jdbc.batchUpdate(
                        "DELETE FROM dish_template_ingredients WHERE dish_key = ?",
                        dishRows,
                        500,
                        (ps, d) -> ps.setString(1, d.dishKey)
                );
            }
        }

        if (!dishRows.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO dish_templates(dish_key, name_en, name_pl, category, default_serving_g) "
                    + "VALUES (?, ?, ?, ?, ?) "
                    + "ON CONFLICT (dish_key) DO UPDATE SET "
                    + "name_en = EXCLUDED.name_en, "
                    + "name_pl = EXCLUDED.name_pl, "
                    + "category = EXCLUDED.category, "
                    + "default_serving_g = EXCLUDED.default_serving_g, "
                    + "updated_at = NOW()",
                    dishRows,
                    500,
                    (ps, d) -> {
                        ps.setString(1, d.dishKey);
                        ps.setString(2, d.nameEn);
                        ps.setString(3, d.namePl);
                        ps.setString(4, d.category);
                        ps.setBigDecimal(5, d.defaultServingG);
                    }
            );
        }

        if (!ingredientRows.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO dish_template_ingredients(dish_key, food_key, name_en, name_pl, default_amount_g, proportion_percent, sort_order) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    ingredientRows,
                    1000,
                    (ps, i) -> {
                        ps.setString(1, i.dishKey);
                        ps.setString(2, i.foodKey);
                        ps.setString(3, i.nameEn);
                        ps.setString(4, i.namePl);
                        ps.setBigDecimal(5, i.defaultAmountG);
                        if (i.proportionFraction == null) ps.setObject(6, null);
                        else ps.setBigDecimal(6, i.proportionFraction);
                        ps.setInt(7, i.sortOrder);
                    }
            );
        }

        if (!dictionaryRows.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO dish_dictionary(term, dish_key, lang) VALUES (?, ?, ?) "
                    + "ON CONFLICT (lang, term) DO UPDATE SET dish_key = EXCLUDED.dish_key",
                    dictionaryRows,
                    2000,
                    (ps, d) -> {
                        ps.setString(1, d.term);
                        ps.setString(2, d.dishKey);
                        ps.setString(3, d.lang);
                    }
            );
        }

        bumpPackVersions();
        log.info("[ETL] Done at {}. dishes={}, ingredients={}, dictionary(en)={}", Instant.now(), dishRows.size(), ingredientRows.size(), dictionaryRows.size());
    }

    private UsdaMatch matchUsda(String ingredientToken, Metrics metrics) {
        List<String> tokenWords = splitWordsForMatch(ingredientToken);
        if (tokenWords.isEmpty()) return null;

        metrics.ingredientMapAttempts++;

        String token = String.join(" ", tokenWords);
        double threshold = similarityThreshold(token);

        String like = buildLikePattern(tokenWords);
        List<UsdaCandidate> candidates = jdbc.query(
                "SELECT external_id, name FROM foods "
                + "WHERE source = 'USDA' AND external_id IS NOT NULL AND LOWER(name) LIKE ? "
                + "ORDER BY LENGTH(name) ASC LIMIT 25",
                (rs, i) -> new UsdaCandidate(rs.getString(1), rs.getString(2)),
                like
        );

        UsdaPick best = pickBestCandidate(token, tokenWords, candidates, threshold);
        if (best != null) {
            metrics.ingredientMapped++;
            return new UsdaMatch("usda:" + best.externalId, best.name);
        }

        String query = token;
        candidates = jdbc.query(
                "SELECT external_id, name FROM foods "
                + "WHERE source = 'USDA' AND external_id IS NOT NULL "
                + "AND to_tsvector('english', name) @@ plainto_tsquery('english', ?) "
                + "ORDER BY ts_rank(to_tsvector('english', name), plainto_tsquery('english', ?)) DESC, LENGTH(name) ASC LIMIT 80",
                (rs, i) -> new UsdaCandidate(rs.getString(1), rs.getString(2)),
                query,
                query
        );

        best = pickBestCandidate(token, tokenWords, candidates, threshold);
        if (best != null) {
            metrics.ingredientMapped++;
            return new UsdaMatch("usda:" + best.externalId, best.name);
        }

        return null;
    }

    private static UsdaPick pickBestCandidate(String token, List<String> tokenWords, List<UsdaCandidate> candidates, double threshold) {
        if (candidates == null || candidates.isEmpty()) return null;

        String tokenSorted = sortWords(tokenWords);
        UsdaPick best = null;

        for (UsdaCandidate c : candidates) {
            if (c == null || c.externalId == null || c.name == null) continue;
            if (isBannedUsdaName(c.name)) continue;

            List<String> candWords = splitWordsForMatch(c.name);
            if (candWords.isEmpty()) continue;

            Set<String> candSet = new HashSet<>(candWords);
            double overlap = overlapRatio(tokenWords, candSet);

            String cn = String.join(" ", candWords);
            double jw = jaroWinkler(tokenSorted, sortWords(candWords));

            double prefixBonus = cn.startsWith(token) ? 1.0 : 0.0;
            double lengthPenalty = Math.min(0.5, cn.length() / 500.0);
            double categoryBoost = preferredCategoryBoost(c.name);

            double score = overlap * 10.0 + prefixBonus + jw + categoryBoost - lengthPenalty;

            if (best == null || score > best.score || (score == best.score && c.name.length() < best.name.length())) {
                best = new UsdaPick(c.externalId, c.name, score, jw, overlap);
            }
        }

        if (best == null) return null;

        if (tokenWords.size() <= 1) {
            return best.jw >= threshold ? best : null;
        }

        if (best.overlap >= 1.0) return best;
        return best.jw >= threshold ? best : null;
    }

    private static double overlapRatio(List<String> tokenWords, Set<String> candidateWords) {
        if (tokenWords == null || tokenWords.isEmpty()) return 0.0;
        int hit = 0;
        for (String w : tokenWords) {
            if (candidateWords.contains(w)) hit++;
        }
        return (double) hit / (double) tokenWords.size();
    }

    private static List<String> splitWordsForMatch(String raw) {
        String n = normalizeForMatch(raw);
        if (n.isBlank()) return List.of();
        return Arrays.stream(n.split(" "))
                .map(String::trim)
                .filter(w -> !w.isBlank())
                .map(RecipeNlgEtlService::singularize)
                .filter(w -> !w.isBlank())
                .filter(w -> !MATCH_STOPWORDS.contains(w))
                .toList();
    }

    private static String singularize(String w) {
        if (w == null) return "";
        String s = w.trim();
        if (s.length() <= 3) return s;
        if (s.endsWith("ies") && s.length() > 4) return s.substring(0, s.length() - 3) + "y";
        if (s.endsWith("s") && !s.endsWith("ss")) return s.substring(0, s.length() - 1);
        return s;
    }

    private static boolean isBannedUsdaName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.contains("restaurant")
                || n.contains("fast foods")
                || n.contains("fast food")
                || n.contains("mcdonald")
                || n.contains("burger king")
                || n.contains("subway")
                || n.contains("taco bell")
                || n.contains("pizza hut")
                || n.contains("kfc")
                || n.contains("wendy");
    }

    private static double preferredCategoryBoost(String rawName) {
        String n = rawName.toLowerCase(Locale.ROOT).trim();
        if (n.startsWith("spices,")) return 2.0;
        if (n.startsWith("sugars,")) return 2.0;
        if (n.startsWith("vegetables,")) return 2.0;
        if (n.startsWith("fruits,")) return 2.0;
        if (n.startsWith("nuts,")) return 2.0;
        if (n.startsWith("legumes,")) return 2.0;
        if (n.startsWith("oils,")) return 2.0;
        if (n.startsWith("fats,")) return 2.0;
        return 0.0;
    }

    private static String sortWords(List<String> words) {
        if (words == null || words.isEmpty()) return "";
        return String.join(" ", words.stream().sorted().toList());
    }

    private static double similarityThreshold(String token) {
        int len = token.replace(" ", "").length();
        int words = token.isBlank() ? 0 : token.split(" ").length;

        if (words <= 1 && len <= 4) return 0.93;
        if (words <= 1 && len <= 6) return 0.90;
        if (len <= 10) return 0.88;
        return 0.85;
    }

    private static String buildLikePattern(List<String> words) {
        StringBuilder sb = new StringBuilder();
        sb.append(words.getFirst());
        for (int i = 1; i < words.size(); i++) {
            sb.append('%').append(words.get(i));
        }
        sb.append('%');
        return sb.toString();
    }

    private void bumpPackVersions() {
        for (String pack : List.of("dish_templates", "lang_en")) {
            jdbc.update("INSERT INTO data_pack_versions(pack_name, version) VALUES (?, 0) ON CONFLICT (pack_name) DO NOTHING", pack);
            jdbc.update("UPDATE data_pack_versions SET version = version + 1, updated_at = NOW() WHERE pack_name = ?", pack);
        }
    }

    private static String generateDishKey(String cleanedTitle, Set<String> used) {
        String base = cleanedTitle
                .replace(' ', '_')
                .replaceAll("[^a-z0-9_]", "_");
        base = collapseRepeats(base, '_').replaceAll("^_+|_+$", "");
        if (base.length() > 50) base = base.substring(0, 50);
        base = base.replaceAll("^_+|_+$", "");
        if (base.isBlank()) base = "dish";

        String key = base;
        int i = 2;
        while (used.contains(key)) {
            String suffix = "_" + i;
            int cut = Math.min(base.length(), 50 - suffix.length());
            key = base.substring(0, cut) + suffix;
            i++;
        }
        used.add(key);
        return key;
    }

    private static int ingredientKindScore(String token) {
        String t = " " + token + " ";
        if (containsAny(t, "beef", "pork", "chicken", "turkey", "fish", "salmon", "tuna", "ham", "bacon", "sausage", "pepperoni", "shrimp")) return 5;
        if (containsAny(t, "cheese", "mozzarella", "cheddar", "parmesan", "feta", "ricotta")) return 4;
        if (containsAny(t, "pasta", "spaghetti", "noodle", "noodles", "rice", "bread", "tortilla", "bun", "flour", "potato")) return 3;
        if (containsAny(t, "onion", "tomato", "lettuce", "spinach", "mushroom", "garlic", "carrot", "cabbage", "broccoli", "pepper", "zucchini")) return 2;
        if (containsAny(t, "sauce", "ketchup", "mustard", "mayo", "mayonnaise", "dressing", "pesto")) return 1;
        return 0;
    }

    private static boolean isGenericIngredient(String token) {
        String t = token.trim();
        if (t.isEmpty()) return true;
        if (GENERIC_INGS.contains(t)) return true;
        return t.endsWith(" pepper") || t.contains(" salt") || t.endsWith(" salt");
    }

    private static int defaultGramsForIngredient(String token) {
        String t = " " + token + " ";

        if (t.contains(" egg") || t.contains(" eggs")) return 100;

        if (containsAny(t, "beef", "pork", "chicken", "turkey", "fish", "salmon", "tuna", "ham", "bacon", "sausage", "pepperoni", "shrimp")) return 120;
        if (containsAny(t, "cheese", "mozzarella", "cheddar", "parmesan", "feta", "ricotta")) return 80;
        if (containsAny(t, "sauce", "ketchup", "mustard", "mayo", "mayonnaise", "dressing", "pesto", "marinara")) return 60;
        if (containsAny(t, "pasta", "spaghetti", "noodle", "noodles", "rice", "bread", "tortilla", "bun", "flour", "potato")) return 150;
        if (containsAny(t, "onion", "tomato", "lettuce", "spinach", "mushroom", "garlic", "carrot", "cabbage", "broccoli", "pepper", "zucchini")) return 80;

        return 100;
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(" " + n + " ") || haystack.contains(" " + n + ",") || haystack.contains("," + n + " ")) return true;
        }
        return false;
    }

    private static String titleCase(String s) {
        if (s == null || s.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (String p : s.split(" ")) {
            if (p.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        return sb.toString();
    }

    private static String cleanTitle(String raw, Metrics metrics) {
        if (raw == null) {
            if (metrics != null) metrics.titleRejectNullOrBlank++;
            return null;
        }

        String s = raw.trim();
        if (s.isEmpty()) {
            if (metrics != null) metrics.titleRejectNullOrBlank++;
            return null;
        }

        s = s.toLowerCase(Locale.ROOT);
        s = stripAccents(s);
        s = removeBracketed(s);
        s = keepAlphaNumSpaces(s);

        List<String> tokens = Arrays.stream(s.split(" "))
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .filter(t -> !TITLE_NOISE_WORDS.contains(t))
                .toList();

        if (tokens.isEmpty()) {
            if (metrics != null) metrics.titleRejectBecameEmpty++;
            return null;
        }

        s = String.join(" ", tokens);

        if (s.startsWith("how to") || s.startsWith("meal prep") || s.startsWith("tips") || s.startsWith("video")) {
            if (metrics != null) metrics.titleRejectPrefix++;
            return null;
        }

        if (s.length() < 4 || s.length() > 60) {
            if (metrics != null) metrics.titleRejectLength++;
            return null;
        }

        if (metrics != null) metrics.titlesKept++;
        return s;
    }

    private static String normalizeIngredientLine(String raw) {
        if (raw == null) return "";

        String s = raw.toLowerCase(Locale.ROOT);
        s = stripAccents(s);
        s = removeBracketed(s);

        s = s.replaceAll("\\bscallions?\\b", "green onion");
        s = s.replaceAll("\\bgreen onions?\\b", "green onion");
        s = s.replaceAll("\\bground beef\\b", "beef ground");
        s = s.replaceAll("\\bbell peppers?\\b", "pepper");
        s = s.replaceAll("\\bmozzarella cheese\\b", "mozzarella");

        s = s.replaceAll("[0-9]", " ");

        s = keepAlphaNumSpaces(s);

        List<String> tokens = new ArrayList<>();
        for (String t : s.split(" ")) {
            if (t.isBlank()) continue;
            if (ING_UNITS.contains(t)) continue;
            if (ING_PREP.contains(t)) continue;
            if (t.length() <= 1) continue;
            tokens.add(t);
        }

        if (tokens.isEmpty()) return "";

        while (!tokens.isEmpty() && (tokens.getLast().equals("or") || tokens.getLast().equals("and"))) {
            tokens.removeLast();
        }

        if (tokens.isEmpty()) return "";

        String token = String.join(" ", tokens).trim();
        token = collapseRepeats(token, ' ');

        if (token.isBlank()) return "";
        if (GENERIC_INGS.contains(token)) return "";

        return token;
    }

    private static String normalizeForMatch(String s) {
        if (s == null) return "";
        s = s.toLowerCase(Locale.ROOT);
        s = stripAccents(s);
        s = removeBracketed(s);
        s = keepAlphaNumSpaces(s);
        return collapseRepeats(s.trim(), ' ');
    }

    private static String stripAccents(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFKD);
        return n.replaceAll("\\p{M}+", "");
    }

    private static String keepAlphaNumSpaces(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean lastSpace = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                out.append(ch);
                lastSpace = false;
            } else if (!lastSpace) {
                out.append(' ');
                lastSpace = true;
            }
        }
        return out.toString().trim();
    }

    private static String removeBracketed(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder(s.length());
        int paren = 0;
        int square = 0;
        int curly = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(') { paren++; continue; }
            if (ch == ')') { if (paren > 0) paren--; continue; }
            if (ch == '[') { square++; continue; }
            if (ch == ']') { if (square > 0) square--; continue; }
            if (ch == '{') { curly++; continue; }
            if (ch == '}') { if (curly > 0) curly--; continue; }
            if (paren > 0 || square > 0 || curly > 0) continue;
            out.append(ch);
        }
        return out.toString();
    }

    private static String collapseRepeats(String s, char c) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean last = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == c) {
                if (last) continue;
                last = true;
            } else {
                last = false;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    private static List<String> parsePythonStringList(String raw) {
        if (raw == null) return List.of();
        String s = raw.trim();
        if (s.isEmpty() || s.equals("[]") || !s.startsWith("[") || !s.endsWith("]")) return List.of();
        List<String> out = new ArrayList<>();
        int i = 1, end = s.length() - 1;
        while (i < end) {
            char ch = s.charAt(i);
            if (ch == ',' || ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') { i++; continue; }
            if (ch == ']') break;
            if (ch != '\'' && ch != '"') { i++; continue; }
            char quote = ch; i++;
            StringBuilder sb = new StringBuilder();
            while (i < end) {
                ch = s.charAt(i);
                if (ch == '\\' && i + 1 < end) { i++; sb.append(s.charAt(i)); i++; continue; }
                if (ch == quote) { i++; break; }
                sb.append(ch); i++;
            }
            if (!sb.isEmpty()) out.add(sb.toString());
        }
        return out;
    }

    private static double jaroWinkler(String s1, String s2) {
        if (Objects.equals(s1, s2)) return 1.0;
        if (s1 == null || s2 == null) return 0.0;

        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 || len2 == 0) return 0.0;

        int matchDistance = Math.max(len1, len2) / 2 - 1;
        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];

        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            for (int j = start; j < end; j++) {
                if (s2Matches[j]) continue;
                if (s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) return 0.0;

        double t = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) t++;
            k++;
        }

        double transpositions = t / 2.0;
        double m = matches;
        double jaro = (m / len1 + m / len2 + (m - transpositions) / m) / 3.0;

        int prefix = 0;
        int maxPrefix = 4;
        for (int i = 0; i < Math.min(Math.min(len1, len2), maxPrefix); i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }

        double p = 0.1;
        return jaro + prefix * p * (1.0 - jaro);
    }

    private record UsdaMatch(String foodKey, String canonicalName) {}
    private record UsdaCandidate(String externalId, String name) {}
    private record UsdaPick(String externalId, String name, double score, double jw, double overlap) {}

    private static final class DishAgg {
        final String cleanedTitle;
        final int globalRecipeCount;
        int recipesConsidered;
        final Map<String, Integer> ingredientCounts = new HashMap<>();

        DishAgg(String cleanedTitle, int globalRecipeCount) {
            this.cleanedTitle = cleanedTitle;
            this.globalRecipeCount = globalRecipeCount;
        }
    }

    private record MappedIngredient(String token, String foodKey, String canonicalName) {}
    private record DishRow(String dishKey, String nameEn, String namePl, String category, BigDecimal defaultServingG) {}
    private record IngredientRow(String dishKey, String foodKey, String nameEn, String namePl, BigDecimal defaultAmountG, BigDecimal proportionFraction, int sortOrder) {}
    private record DictionaryRow(String term, String dishKey, String lang) {}
    private record ExampleDish(String dishKey, String term, List<MappedIngredient> mapped) {}

    private static final class Metrics {
        long totalRows;
        long titlesKept;

        long titleRejectNullOrBlank;
        long titleRejectPrefix;
        long titleRejectLength;
        long titleRejectBecameEmpty;

        long ingredientLinesTotal;
        long ingredientTokenEmpty;

        long dishRejectNoRecipes;
        long dishRejectNoIngredientsAfterSupport;
        long dishRejectTooFewIngredients;
        long dishRejectTooFewMappedIngredients;

        long ingredientMapAttempts;
        long ingredientMapped;
        long ingredientUnmapped;

        long dishesKept;
        long ingredientsKept;

        final List<ExampleDish> exampleDishes = new ArrayList<>();
    }
}
