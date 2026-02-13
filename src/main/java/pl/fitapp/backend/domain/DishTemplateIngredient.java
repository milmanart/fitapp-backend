package pl.fitapp.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "dish_template_ingredients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DishTemplateIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_key", nullable = false)
    private DishTemplate dishTemplate;

    @Column(name = "food_key", nullable = false, length = 128)
    private String foodKey;

    @Column(name = "name_en", nullable = false, length = 256)
    private String nameEn;

    @Column(name = "name_pl", length = 256)
    private String namePl;

    @Column(name = "default_amount_g", nullable = false)
    private BigDecimal defaultAmountG;

    @Column(name = "proportion_percent")
    private BigDecimal proportionPercent;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
