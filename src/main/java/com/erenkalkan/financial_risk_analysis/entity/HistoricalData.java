package com.erenkalkan.financial_risk_analysis.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Historical_Data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalData {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name="asset_id", nullable = false)
    private Asset asset;

    @Column(name="purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name="today", nullable = false)
    private LocalDate currentDate = LocalDate.now();

    @Column(name="purchase_price",nullable = false)
    private Double purchasePrice;

    @Column(name="current_price", nullable = false)
    private Double currentPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}
