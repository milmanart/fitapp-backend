package pl.fitapp.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "dish_dictionary",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_dish_dictionary_lang_term", columnNames = {"lang", "term"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DishDictionaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 256)
    private String term;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_key", nullable = false)
    private DishTemplate dishTemplate;

    @Column(nullable = false, length = 5)
    private String lang;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
