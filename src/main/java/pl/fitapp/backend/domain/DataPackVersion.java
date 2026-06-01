package pl.fitapp.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_pack_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataPackVersion {

    @Id
    @Column(name = "pack_name", length = 64)
    private String packName;

    @Column(nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
