package pl.fitapp.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dish_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishTemplate {

    @Id
    @Column(name = "dish_key", length = 128)
    private String dishKey;

    @Column(name = "name_en", nullable = false, length = 256)
    private String nameEn;

    @Column(name = "name_pl", length = 256)
    private String namePl;

    @Column(length = 64)
    private String category;

    @Column(name = "default_serving_g", nullable = false)
    private BigDecimal defaultServingG;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "dishTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DishTemplateIngredient> ingredients = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
