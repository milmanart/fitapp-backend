package pl.fitapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.fitapp.backend.domain.DishDictionaryEntry;

import java.util.List;
import java.util.UUID;

@Repository
public interface DishDictionaryRepository extends JpaRepository<DishDictionaryEntry, UUID> {
    List<DishDictionaryEntry> findByLangOrderByTermAsc(String lang);
}
