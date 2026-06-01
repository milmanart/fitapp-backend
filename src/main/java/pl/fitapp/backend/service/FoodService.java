package pl.fitapp.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.fitapp.backend.domain.Food;
import pl.fitapp.backend.domain.FoodSource;
import pl.fitapp.backend.repository.FoodRepository;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FoodService {

    private static final Set<String> FAST_FOOD_HINTS = Set.of(
            "mcdonald", "mcdonald's", "mcdonalds", "big mac", "quarter pounder", "mcchicken", "mcnugget", "filet-o-fish", "mcflurry", "mcdouble",
            "burger king", "whopper", "bacon king", "chicken fries", "bk",
            "kfc", "kentucky fried chicken", "kentucky", "zinger", "twister", "famous bowl",
            "wendy's", "wendys", "dave's single", "baconator",
            "taco bell", "crunchwrap", "quesadilla", "doritos locos",
            "subway", "bmt", "italian b.m.t", "meatball marinara",
            "pizza hut", "domino's", "dominos", "papa john", "little caesars",
            "chick-fil-a", "chick fil a", "popeyes", "arbys", "five guys"
    );

    private static final List<String> POPULAR_FASTFOOD_QUERIES = List.of(
            "McDONALD'S, BIG MAC",
            "McDONALD'S, CHEESEBURGER",
            "McDONALD'S, HAMBURGER",
            "McDONALD'S, Quarter Pounder with Cheese",
            "McDONALD'S, McCHICKEN",
            "McDONALD'S, FILET-O-FISH",
            "McDONALD'S, French Fries",

            "BURGER KING, WHOPPER, with cheese",
            "BURGER KING, WHOPPER, no cheese",
            "BURGER KING, Cheeseburger",
            "BURGER KING, French Fries",
            "BURGER KING, Chicken Nuggets",

            "KFC, Fried Chicken, ORIGINAL RECIPE, Breast, meat and skin with breading",
            "KFC, Fried Chicken, ORIGINAL RECIPE, Drumstick, meat and skin with breading",
            "KFC, Fried Chicken, EXTRA CRISPY, Breast, meat and skin with breading",
            "KFC, Coleslaw",
            "KFC, Biscuit"
    );

    private static final List<ChainSeedSpec> FAST_FOOD_CHAIN_SEEDS = List.of(
            new ChainSeedSpec("mcdonald", "McDONALD'S", 100, 8, 250),
            new ChainSeedSpec("mcdonald", "McDONALD'S, Quarter Pounder", 60, 4, 250),
            new ChainSeedSpec("mcdonald", "McDONALD'S, McCHICKEN", 60, 4, 250),
            new ChainSeedSpec("mcdonald", "McDONALD'S, McNUGGETS", 60, 4, 250),
            new ChainSeedSpec("mcdonald", "McDONALD'S, Filet-O-Fish", 40, 3, 250),
            new ChainSeedSpec("mcdonald", "McDONALD'S, Sausage", 40, 3, 250),
            new ChainSeedSpec("mcdonald", "McDONALD'S, McFLURRY", 40, 3, 250),
            new ChainSeedSpec("mcdonald", "McDONALD'S, French Fries", 40, 3, 250),

            new ChainSeedSpec("burger king", "BURGER KING", 100, 8, 180),
            new ChainSeedSpec("burger king", "BURGER KING, WHOPPER", 60, 4, 180),
            new ChainSeedSpec("burger king", "BURGER KING, Chicken", 60, 4, 180),
            new ChainSeedSpec("burger king", "BURGER KING, Fries", 40, 3, 180),
            new ChainSeedSpec("burger king", "BURGER KING, Breakfast", 40, 3, 180),

            new ChainSeedSpec("kfc", "KFC", 100, 8, 180),
            new ChainSeedSpec("kfc", "KFC, Fried Chicken", 80, 6, 180),
            new ChainSeedSpec("kfc", "KFC, Chicken", 60, 4, 180),
            new ChainSeedSpec("kfc", "KFC, Crispy", 60, 4, 180),
            new ChainSeedSpec("kfc", "KFC, Biscuit", 40, 3, 180),
            new ChainSeedSpec("kfc", "KFC, Coleslaw", 40, 3, 180),

            new ChainSeedSpec("wendy", "WENDY'S", 80, 6, 80),
            new ChainSeedSpec("taco bell", "TACO BELL", 80, 6, 80),
            new ChainSeedSpec("subway", "SUBWAY", 80, 6, 80),
            new ChainSeedSpec("chick-fil-a", "CHICK-FIL-A", 80, 6, 80),
            new ChainSeedSpec("popeyes", "POPEYES", 80, 6, 80),
            new ChainSeedSpec("domino", "DOMINO'S", 80, 6, 80),
            new ChainSeedSpec("pizza hut", "PIZZA HUT", 80, 6, 80)
    );

    private static final long USDA_MISS_COOLDOWN_MILLIS = 30L * 60L * 1000L;

    private record ChainSeedSpec(String localHint, String usdaQuery, int fetchLimit, int maxPages, int minTarget) {
    }

    private final FoodRepository foodRepository;
    private final OpenFoodFactsClient openFoodFactsClient;
    private final UsdaFdcClient usdaFdcClient;

    private final Map<String, Long> offMissCache  = new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.time.Duration OFF_CACHE_TTL = java.time.Duration.ofDays(7);

    public List<Food> searchFoods(String query, String locale, int limit, boolean remote) {
        log.info("Searching foods: query='{}', locale='{}', limit={}", query, locale, limit);

        List<Food> localResults = new ArrayList<>(foodRepository.findByNameContainingIgnoreCase(query));
        log.info("Found {} local results for query: {}", localResults.size(), query);

        int effectiveLimit = Math.max(1, limit);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean fastFoodQuery = isFastFoodQuery(normalizedQuery);

        if (fastFoodQuery && localResults.isEmpty()) {
            List<Food> fullTextMatches = foodRepository.searchByNameFullText(query, Math.max(10, effectiveLimit * 3));
            localResults.addAll(fullTextMatches);
        }

        if (!remote) {
            return new ArrayList<>(localResults);
        }

        if (fastFoodQuery) {
            localResults.sort(Comparator
                    .comparingInt(this::fastFoodSourcePriority)
                    .thenComparing(Food::getName, String.CASE_INSENSITIVE_ORDER));
        }

        List<Food> cachedUsda = List.of();
        if (shouldQueryUsda(normalizedQuery, localResults, effectiveLimit)) {
            List<Food> usdaResults = usdaFdcClient.searchByName(query, effectiveLimit);
            cachedUsda = cacheUsdaResults(usdaResults);
        }

        Map<String, Food> preCheck = new LinkedHashMap<>();
        for (Food f : localResults)  preCheck.putIfAbsent(dedupeKey(f), f);
        for (Food f : cachedUsda)    preCheck.putIfAbsent(dedupeKey(f), f);

        List<Food> cachedOff = List.of();
        if (shouldQueryOff(normalizedQuery, preCheck.values())) {
            log.info("[OFF] Last-resort search for query: '{}' (local={}, usda={})",
                    query, localResults.size(), cachedUsda.size());
            List<Food> offResults = openFoodFactsClient.searchByName(query, effectiveLimit);
            if (offResults.isEmpty()) {
                offMissCache.put(normalizedQuery, System.currentTimeMillis());
            } else {
                offMissCache.remove(normalizedQuery);
            }
            cachedOff = cacheOffResults(offResults);
        }

        Map<String, Food> merged = new LinkedHashMap<>();
        for (Food f : localResults) {
            merged.putIfAbsent(dedupeKey(f), f);
        }
        for (Food f : cachedUsda) {
            merged.putIfAbsent(dedupeKey(f), f);
        }
        for (Food f : cachedOff) {
            merged.putIfAbsent(dedupeKey(f), f);
        }

        List<Food> result = new ArrayList<>(merged.values());
        if (result.size() > effectiveLimit) {
            return result.subList(0, effectiveLimit);
        }
        return result;
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

        if (key.startsWith("off:")) {
            return getFoodByExternalId(key.substring("off:".length()), FoodSource.OPENFOODFACTS);
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

        Optional<Food> local = foodRepository.findByBarcode(barcode);
        if (local.isPresent()) {
            Food existing = local.get();

            if (existing.getSource() == FoodSource.OPENFOODFACTS && isBarcodeCacheStale(existing)) {
                log.info("Barcode {} cached OFF copy is stale; refreshing from OpenFoodFacts", barcode);
                Optional<Food> refresh = openFoodFactsClient.fetchByBarcode(barcode);
                if (refresh.isPresent()) {
                    Food refreshed = refresh.get();
                    refreshed.setId(existing.getId());
                    Food saved = foodRepository.save(refreshed);
                    return Optional.of(saved);
                }

                log.warn("OpenFoodFacts refresh failed for barcode {}, using cached value", barcode);
                return local;
            }

            log.info("Found barcode {} in local DB (cache hit)", barcode);
            return local;
        }

        log.info("Barcode {} not in DB, querying OpenFoodFacts...", barcode);
        Optional<Food> offResult = openFoodFactsClient.fetchByBarcode(barcode);
        if (offResult.isPresent()) {
            Food saved = foodRepository.save(offResult.get());
            log.info("Cached OFF product '{}' for barcode {}", saved.getName(), barcode);
            return Optional.of(saved);
        }

        log.info("No food found for barcode: {}", barcode);
        return Optional.empty();
    }

    private boolean isBarcodeCacheStale(Food food) {
        java.time.LocalDateTime ref = food.getLastSyncedAt() != null
                ? food.getLastSyncedAt()
                : (food.getUpdatedAt() != null ? food.getUpdatedAt() : food.getCreatedAt());
        if (ref == null) return true;
        return ref.isBefore(java.time.LocalDateTime.now().minus(OFF_CACHE_TTL));
    }

    public List<Food> getFoodsBySource(FoodSource source) {
        return foodRepository.findBySource(source);
    }


    public long getTotalCount() {
        return foodRepository.count();
    }

    public long getCountBySource(FoodSource source) {
        return foodRepository.countBySource(source);
    }

    public int warmupPopularFastFood() {
        int inserted = 0;

        for (String query : POPULAR_FASTFOOD_QUERIES) {
            inserted += preloadFastFoodQuery(query);
        }

        int chainInserted = preloadFastFoodChains();
        inserted += chainInserted;

        log.info("[USDA] Fastfood warmup finished: inserted={}, chainInserted={}", inserted, chainInserted);
        return inserted;
    }

    private int preloadFastFoodChains() {
        int inserted = 0;

        for (ChainSeedSpec spec : FAST_FOOD_CHAIN_SEEDS) {
            int existingUsdaForChain = countUsdaByNameHint(spec.localHint());
            if (existingUsdaForChain >= spec.minTarget()) {
                log.info("[USDA] Chain '{}' already seeded ({} >= {}), skipping",
                        spec.localHint(), existingUsdaForChain, spec.minTarget());
                continue;
            }

            List<Food> fetched = usdaFdcClient.searchByName(spec.usdaQuery(), spec.fetchLimit(), spec.maxPages());
            if (fetched.isEmpty()) {
                log.warn("[USDA] Chain seed query returned no results: {}", spec.usdaQuery());
                continue;
            }

            int insertedForChain = 0;
            for (Food food : fetched) {
                if (!belongsToChain(food, spec.localHint())) {
                    continue;
                }
                if (saveUsdaIfAbsent(food)) {
                    insertedForChain++;
                }
            }

            inserted += insertedForChain;
            log.info("[USDA] Chain '{}' seeded {} new items", spec.localHint(), insertedForChain);
        }

        return inserted;
    }

    private int countUsdaByNameHint(String localHint) {
        return (int) foodRepository.findByNameContainingIgnoreCase(localHint).stream()
                .filter(f -> f.getSource() == FoodSource.USDA)
                .count();
    }

    private boolean belongsToChain(Food food, String localHint) {
        if (food == null) {
            return false;
        }

        String needle = localHint.toLowerCase(Locale.ROOT);
        String normalizedName = food.getName() != null
                ? food.getName().toLowerCase(Locale.ROOT)
                : "";
        String normalizedBrand = food.getBrand() != null
                ? food.getBrand().toLowerCase(Locale.ROOT)
                : "";

        return normalizedName.contains(needle) || normalizedBrand.contains(needle);
    }

    private boolean saveUsdaIfAbsent(Food food) {
        if (food.getExternalId() == null || food.getExternalId().isBlank()) {
            return false;
        }

        if (foodRepository.findByExternalIdAndSource(food.getExternalId(), FoodSource.USDA).isPresent()) {
            return false;
        }

        try {
            foodRepository.save(food);
            return true;
        } catch (Exception e) {
            log.warn("Failed to save USDA food '{}': {}", food.getName(), e.getMessage());
            return false;
        }
    }

    private int preloadFastFoodQuery(String query) {
        if (!foodRepository.findByNameContainingIgnoreCase(query).isEmpty()) {
            return 0;
        }

        List<Food> fetched = usdaFdcClient.searchByName(query, 1);
        if (fetched.isEmpty()) {
            log.warn("[USDA] No results for popular query: {}", query);
            return 0;
        }

        Food candidate = fetched.getFirst();
        if (candidate.getExternalId() == null || candidate.getExternalId().isBlank()) {
            return 0;
        }

        boolean inserted = saveUsdaIfAbsent(candidate);
        if (inserted) {
            log.info("[USDA] Seeded popular fastfood: {}", candidate.getName());
            return 1;
        }

        return 0;
    }

    private boolean shouldQueryUsda(String normalizedQuery, List<Food> localResults, int limit) {
        if (normalizedQuery == null || normalizedQuery.length() < 3 || limit <= 0) {
            return false;
        }

        long usdaCount = localResults.stream()
                .filter(f -> f.getSource() == FoodSource.USDA)
                .count();

        boolean fastFoodQuery = isFastFoodQuery(normalizedQuery);
        boolean hasNoLocalMatches = localResults.isEmpty();

        return fastFoodQuery ? usdaCount < Math.max(1, Math.min(3, limit)) : hasNoLocalMatches;
    }

    private boolean shouldQueryOff(String normalizedQuery, java.util.Collection<Food> combined) {
        if (normalizedQuery == null || normalizedQuery.length() < 3) return false;
        Long missedAt = offMissCache.get(normalizedQuery);
        if (missedAt != null && (System.currentTimeMillis() - missedAt) < USDA_MISS_COOLDOWN_MILLIS) {
            log.debug("[OFF] Skipping '{}' — in miss cooldown", normalizedQuery);
            return false;
        }
        if (!combined.isEmpty()) {
            log.debug("[OFF] Skipping '{}' — already have {} local/USDA results", normalizedQuery, combined.size());
            return false;
        }
        return true;
    }

    private boolean isFastFoodQuery(String normalizedQuery) {
        for (String hint : FAST_FOOD_HINTS) {
            if (normalizedQuery.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private int fastFoodSourcePriority(Food food) {
        if (food.getSource() == FoodSource.USDA) {
            return 0;
        }
        if (food.getSource() == FoodSource.MANUAL) {
            return 1;
        }
        if (food.getSource() == FoodSource.OPENFOODFACTS) {
            return 2;
        }
        return 3;
    }

    private String dedupeKey(Food food) {
        if (food.getBarcode() != null && !food.getBarcode().isBlank()) {
            return "barcode:" + food.getBarcode();
        }

        if (food.getSource() != null && food.getExternalId() != null && !food.getExternalId().isBlank()) {
            return food.getSource().name() + ":" + food.getExternalId();
        }

        if (food.getId() != null) {
            return "local:" + food.getId();
        }

        return "name:" + food.getName().toLowerCase(Locale.ROOT);
    }

    private List<Food> cacheUsdaResults(List<Food> usdaFoods) {
        List<Food> cached = new ArrayList<>();
        for (Food f : usdaFoods) {
            if (f.getExternalId() == null || f.getExternalId().isBlank()) continue;

            Optional<Food> existing = foodRepository.findByExternalIdAndSource(f.getExternalId(), FoodSource.USDA);
            if (existing.isPresent()) {
                cached.add(existing.get());
            } else {
                try {
                    cached.add(foodRepository.save(f));
                } catch (Exception e) {
                    log.warn("Failed to cache USDA food '{}': {}", f.getName(), e.getMessage());
                }
            }
        }
        return cached;
    }

    private List<Food> cacheOffResults(List<Food> offFoods) {
        List<Food> cached = new ArrayList<>();
        for (Food f : offFoods) {
            if (f.getBarcode() == null) continue;
            Optional<Food> existing = foodRepository.findByBarcode(f.getBarcode());
            if (existing.isPresent()) {
                cached.add(existing.get());
            } else {
                try {
                    cached.add(foodRepository.save(f));
                } catch (Exception e) {
                    log.warn("Failed to cache OFF food '{}': {}", f.getName(), e.getMessage());
                }
            }
        }
        return cached;
    }
}
