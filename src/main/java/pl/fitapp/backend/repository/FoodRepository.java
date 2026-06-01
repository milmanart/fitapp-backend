package pl.fitapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.fitapp.backend.domain.Food;
import pl.fitapp.backend.domain.FoodSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FoodRepository extends JpaRepository<Food, UUID> {

    Optional<Food> findByBarcode(String barcode);

    Optional<Food> findByExternalIdAndSource(String externalId, FoodSource source);

    List<Food> findByExternalIdInAndSource(List<String> externalIds, FoodSource source);

    long countBySource(FoodSource source);

    @Query("SELECT f FROM Food f WHERE (LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(COALESCE(f.brand, '')) LIKE LOWER(CONCAT('%', :query, '%'))) AND f.source = :source ORDER BY f.name")
    List<Food> findByNameContainingIgnoreCaseAndSource(@Param("query") String query, @Param("source") FoodSource source);

    @Query("SELECT f FROM Food f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(COALESCE(f.brand, '')) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY f.name")
    List<Food> findByNameContainingIgnoreCase(@Param("query") String query);

    @Query("SELECT f FROM Food f WHERE f.source = :source")
    List<Food> findBySource(@Param("source") FoodSource source);

    @Query(value = "SELECT * FROM foods WHERE to_tsvector('english', name) @@ plainto_tsquery('english', :query) ORDER BY name LIMIT :limit", nativeQuery = true)
    List<Food> searchByNameFullText(@Param("query") String query, @Param("limit") int limit);
}
