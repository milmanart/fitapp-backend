package pl.fitapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.fitapp.backend.domain.DishTemplate;

@Repository
public interface DishTemplateRepository extends JpaRepository<DishTemplate, String> {
}
