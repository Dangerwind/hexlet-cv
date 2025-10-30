package io.hexlet.cv.model.marketing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "marketing_pricing_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PricingPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "original_price", nullable = false)
    private Double originalPrice = 0.0;

    @Column(name = "discount_percent", nullable = false)
    private Double discountPercent = 0.0;

    @Column(name = "final_price")
    private Double finalPrice = 0.0;

    @Column(nullable = false, length = 1000)
    private String description;

    @PrePersist
    @PreUpdate
    public void calculateFinalPrice() {
        if (originalPrice == null) {
            this.finalPrice = null;
            return;
        }

        if (originalPrice < 0) {
            throw new IllegalArgumentException("Original price cannot be negative");
        }

        if (discountPercent == null || discountPercent <= 0) {
            this.finalPrice = originalPrice;
            return;
        }

        if (discountPercent < 0) {
            throw new IllegalArgumentException("Discount percent cannot be negative");
        }

        if (discountPercent >= 100) {
            this.finalPrice = 0.0;
            return;
        }

        double discountAmount = originalPrice * (discountPercent / 100);
        double unroundedPrice = originalPrice - discountAmount;
        this.finalPrice = Math.round(unroundedPrice * 100.0) / 100.0;

        if (this.finalPrice < 0) {
            this.finalPrice = 0.0;
        }
    }
}
