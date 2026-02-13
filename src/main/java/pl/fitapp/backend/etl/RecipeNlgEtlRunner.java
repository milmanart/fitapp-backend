package pl.fitapp.backend.etl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("etl")
@RequiredArgsConstructor
public class RecipeNlgEtlRunner implements CommandLineRunner {

    private final RecipeNlgEtlService etlService;

    @Value("${etl.recipenlg.csv-path}")
    private String csvPath;

    @Value("${etl.recipenlg.top-dishes:5000}")
    private int topDishes;

    @Value("${etl.recipenlg.min-recipes-per-dish:50}")
    private int minRecipesPerDish;

    @Value("${etl.recipenlg.min-ingredient-support:0.4}")
    private double minIngredientSupport;

    @Value("${etl.recipenlg.max-ingredients:8}")
    private int maxIngredients;

    @Value("${etl.recipenlg.default-serving-g:300}")
    private int defaultServingG;

    @Value("${etl.recipenlg.truncate:true}")
    private boolean truncate;

    @Value("${etl.recipenlg.dry-run:false}")
    private boolean dryRun;

    @Override
    public void run(String... args) throws Exception {
        etlService.run(
                csvPath,
                topDishes,
                minRecipesPerDish,
                minIngredientSupport,
                maxIngredients,
                defaultServingG,
                truncate,
                dryRun
        );
    }
}
