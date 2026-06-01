package pl.fitapp.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FastFoodSeedService {

    private final FoodService foodService;

    @Value("${usda.fdc.seed-on-startup:true}")
    private boolean seedOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    public void seedFastFoodOnStartup() {
        if (!seedOnStartup) {
            log.info("[USDA] Fastfood startup seed disabled");
            return;
        }

        int inserted = foodService.warmupPopularFastFood();
        log.info("[USDA] Fastfood startup seed completed: inserted={}", inserted);
    }
}
