package pl.fitapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.fitapp.backend.domain.DishTemplateIngredient;

import java.util.List;
import java.util.UUID;

@Repository
public interface DishTemplateIngredientRepository extends JpaRepository<DishTemplateIngredient, UUID> {
    List<DishTemplateIngredient> findByDishTemplate_DishKeyOrderBySortOrderAsc(String dishKey);

    @Query("SELECT DISTINCT i.foodKey FROM DishTemplateIngredient i")
    List<String> findDistinctFoodKeys();
}
