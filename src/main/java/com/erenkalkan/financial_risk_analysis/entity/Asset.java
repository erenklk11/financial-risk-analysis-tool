package com.erenkalkan.financial_risk_analysis.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Assets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "country_code")
    private String countryCode;

    @Column(nullable = false)
    private Double quantity;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name="purchase_price", nullable = false)
    private Double purchasePrice;

    @Column(name = "current_price", nullable = false)
    private Double currentPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public String toString() {
        return "Asset{" +
                "id=" + id +
                ", portfolio=" + portfolio.getId() +
                ", name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", quantity=" + quantity +
                ", purchaseDate=" + purchaseDate +
                ", purchasePrice=" + purchasePrice +
                ", currentPrice=" + currentPrice +
                ", createdAt=" + createdAt +
                '}';
    }
}