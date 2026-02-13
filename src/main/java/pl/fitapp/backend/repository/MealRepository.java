package pl.fitapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.fitapp.backend.domain.Meal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MealRepository extends JpaRepository<Meal, UUID> {
    List<Meal> findByUserIdOrderByEatenAtDesc(UUID userId);
    
    List<Meal> findByUserIdAndEatenAtBetweenOrderByEatenAtDesc(UUID userId, LocalDateTime start, LocalDateTime end);
}
