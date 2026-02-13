package pl.fitapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.fitapp.backend.domain.DataPackVersion;

@Repository
public interface DataPackVersionRepository extends JpaRepository<DataPackVersion, String> {
}
