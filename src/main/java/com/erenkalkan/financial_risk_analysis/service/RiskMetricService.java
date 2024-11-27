package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.RiskMetric;

import java.util.List;

public interface RiskMetricService {

    RiskMetric findByPortfolio(Portfolio portfolio);

    void save(RiskMetric riskMetric);

    void deleteById(Long id);

    double calculateVolatility(List<Double> investmentReturns);

    double calculateSharpeRatio(List<Double> investmentReturns, double riskFreeRate);

    double calculateBeta(List<Double> investmentReturns, List<Double> marketReturns);

    double calculateAlpha(double portfolioReturn, double marketReturn, double riskFreeRate, double beta);

    double calculateValueAtRisk(double mean, double stdDev, double zScore);

    double calculateMaximumDrawdown(List<Double> portfolioValues);
}
