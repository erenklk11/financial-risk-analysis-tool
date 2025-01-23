package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.RiskMetric;

import java.util.List;
import java.util.Map;

public interface RiskMetricService {


    RiskMetric findByPortfolio(Portfolio portfolio);

    void save(RiskMetric riskMetric);

    void deleteById(Long id);


    double calculateSharpeRatio(List<Double> investmentReturns, double riskFreeRate, double portfolioVolatility);

    double calculatePortfolioBeta(Portfolio portfolio, Map<Asset, List<Double>> historicalAssetPrices, List<Double> marketReturns);

    double calculateAlpha(double portfolioReturn, double marketReturn, double riskFreeRate, double beta);

    double calculateValueAtRisk(double meanReturn, double portfolioVolitalityWithCorrelation, double zScore);

    double calculateMaximumDrawdown(List<Double> portfolioValues);
}
