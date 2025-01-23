package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.RiskMetric;
import com.erenkalkan.financial_risk_analysis.repository.RiskMetricRepository;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskMetricServiceImpl implements RiskMetricService {


    private final RiskMetricRepository riskMetricRepository;


    @Override
    public RiskMetric findByPortfolio(Portfolio portfolio) {
        return riskMetricRepository.findByPortfolio(portfolio);
    }

    @Override
    public void save(RiskMetric riskMetric) {
        riskMetricRepository.save(riskMetric);
    }

    @Override
    public void deleteById(Long id) {
        riskMetricRepository.deleteById(id);
    }



    /**
     * Calculate Sharpe Ratio.
     *
     * @param investmentReturns List of investment investmentReturns
     * @return Sharpe Ratio
     */
    public double calculateSharpeRatio(List<Double> investmentReturns, double riskFreeRate, double portfolioVolatility) {

        if (investmentReturns == null || investmentReturns.isEmpty()) {
            throw new IllegalArgumentException("Returns list cannot be null or empty");
        }

        // Convert annual risk-free rate to daily risk-free rate
        double dailyRiskFreeRate = riskFreeRate / (252*100); // Convert to decimal and daily

        double meanReturn = investmentReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Convert annual portfolio volatility to daily volatility
        double dailyPortfolioVolatility = portfolioVolatility / Math.sqrt(252);

        return (meanReturn - dailyRiskFreeRate) / dailyPortfolioVolatility;
    }

    /**
     * Calculate the beta of an investment.
     *
     * @param marketReturns List of market benchmark returns
     * @return Beta
     */
    @Override
    public double calculatePortfolioBeta(Portfolio portfolio, Map<Asset, List<Double>> assetReturns, List<Double> marketReturns) {

        List<Asset> assets = portfolio.getAssets();

        double portfolioBeta = 0.0;

        for(Asset asset : assets) {
            double assetBeta = calculateBeta(assetReturns.get(asset), marketReturns);
            portfolioBeta += asset.getWeight() * assetBeta;
        }

        return portfolioBeta;
    }

    private double calculateBeta(List<Double> assetReturns, List<Double> marketReturns) {
        if (assetReturns == null || marketReturns == null || assetReturns.size() != marketReturns.size()) {
            throw new IllegalArgumentException("Returns lists must be non-null and of the same size");
        }

        double assetMean = assetReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double marketMean = marketReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double covariance = 0.0;
        double marketVariance = 0.0;
        for (int i = 0; i < assetReturns.size(); i++) {
            double assetDiff = assetReturns.get(i) - assetMean;
            double marketDiff = marketReturns.get(i) - marketMean;

            covariance += assetDiff * marketDiff;
            marketVariance += marketDiff * marketDiff;
        }

        covariance /= assetReturns.size();
        marketVariance /= marketReturns.size();

        // Ensure market variance is not zero
        if (marketVariance == 0) {
            throw new IllegalArgumentException("Market variance cannot be zero");
        }

        // Return beta, the ratio of covariance to market variance
        return covariance / marketVariance;
    }


    /**
     * Calculate Alpha.
     *
     * @param portfolioReturn Portfolio's return
     * @param marketReturn Market return
     * @param riskFreeRate Risk-free rate
     * @param beta Portfolio's beta
     * @return Alpha
     */
    @Override
    public double calculateAlpha(double portfolioReturn, double marketReturn, double riskFreeRate, double beta) {

        double annualizedPortfolioReturn = portfolioReturn * 252;
        double annualizedMarketReturn = marketReturn * 252;
        double annualizedRiskFreeRate = riskFreeRate * 252;  // Assuming daily risk-free rate

        return annualizedPortfolioReturn - (annualizedRiskFreeRate + beta * (annualizedMarketReturn - annualizedRiskFreeRate));
    }

    /**
     * Calculate Value at Risk (VaR).
     *
     * @param meanReturn       Mean return of the portfolio
     * @param portfolioVolatilityWithCorrelation Standard deviation of portfolio returns
     * @param zScore               Z-score for the desired confidence level (e.g., 1.645 for 95%)
     * @return Value at Risk (VaR)
     */
    @Override
    public double calculateValueAtRisk(double meanReturn, double portfolioVolatilityWithCorrelation, double zScore) {
        // Annualize the mean return and volatility
        double annualizedMeanReturn = meanReturn * 252;
        double annualizedVolatility = portfolioVolatilityWithCorrelation * Math.sqrt(252);

        return annualizedMeanReturn - zScore * annualizedVolatility;
    }

    /**
     * Calculate Maximum Drawdown (MDD).
     *
     * @param portfolioValues List of portfolio values over time
     * @return Maximum Drawdown (MDD)
     */
    @Override
    public double calculateMaximumDrawdown(List<Double> portfolioValues) {
        if (portfolioValues == null || portfolioValues.isEmpty()) {
            throw new IllegalArgumentException("Portfolio values list cannot be null or empty");
        }

        double maxDrawdown = 0.0;
        double peak = portfolioValues.get(0);

        for (double value : portfolioValues) {
            if (value > peak) {
                peak = value;
            }
            double drawdown = (peak - value) / peak;
            maxDrawdown = Math.max(maxDrawdown, drawdown);
        }

        return maxDrawdown;
    }

}
