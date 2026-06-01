package pl.fitapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.fitapp.backend.domain.DishDictionaryEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DishDictionaryRepository extends JpaRepository<DishDictionaryEntry, UUID> {
    List<DishDictionaryEntry> findByLangOrderByTermAsc(String lang);

    Optional<DishDictionaryEntry> findFirstByTermIgnoreCaseAndLang(String term, String lang);

    @Query("SELECT d FROM DishDictionaryEntry d WHERE LOWER(d.term) LIKE LOWER(CONCAT('%', :term, '%')) AND d.lang = :lang ORDER BY LENGTH(d.term) ASC")
    List<DishDictionaryEntry> findByTermContainingIgnoreCaseAndLang(@Param("term") String term, @Param("lang") String lang);
}
