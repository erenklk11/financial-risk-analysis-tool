package com.erenkalkan.financial_risk_analysis.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Risk_Metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskMetric {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = true)
    private Double volatility;

    @Column(name = "sharpe_ratio", nullable = true)
    private Double sharpeRatio;

    @Column(nullable = true)
    private Double beta;

    @Column(nullable = true)
    private Double alpha;

    @Column(nullable = true)
    private Double mdd; // Maximum Drawdown

    @Column(nullable = true)
    private Double var; // Value at Risk

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
