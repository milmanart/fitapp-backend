package pl.fitapp.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "foods")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Food {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "external_id")
    private String externalId;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FoodSource source;
    
    private String barcode;
    
    @Column(nullable = false, length = 500)
    private String name;
    
    private String brand;
    
    @Column(name = "serving_size")
    private BigDecimal servingSize;
    
    @Column(name = "serving_unit")
    private String servingUnit;
    
    @Column(nullable = false)
    private BigDecimal calories;
    
    @Column(nullable = false)
    private BigDecimal protein;
    
    @Column(nullable = false)
    private BigDecimal fat;
    
    @Column(nullable = false)
    private BigDecimal carbohydrates;
    
    private BigDecimal fiber;
    private BigDecimal sugar;
    private BigDecimal sodium;
    
    private String language;
    
    @Column(name = "country_codes")
    private String countryCodes;
    
    @Column(columnDefinition = "TEXT")
    private String categories;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
    
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
