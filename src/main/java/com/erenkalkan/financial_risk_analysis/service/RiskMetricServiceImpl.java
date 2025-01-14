package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.RiskMetric;
import com.erenkalkan.financial_risk_analysis.repository.RiskMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
     * Calculate the standard deviation of returns.
     *
     * @param investmentReturns List of investment returns
     * @return Standard deviation
     */
    @Override
    public double calculateVolatility(List<Double> investmentReturns) {

        if (investmentReturns == null || investmentReturns.isEmpty()) {
            throw new IllegalArgumentException("Returns list cannot be null or empty");
        }

        double mean = investmentReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double variance = investmentReturns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .average()
                .orElse(0.0);

        // Calculate annualized volatility
        // Assuming 252 trading days per year for daily returns
        // Multiply by 100 to express as percentage
        return Math.sqrt(variance) * Math.sqrt(252) * 100;
    }

    /**
     * Calculate Sharpe Ratio.
     *
     * @param investmentReturns List of investment investmentReturns
     * @return Sharpe Ratio
     */
    public double calculateSharpeRatio(List<Double> investmentReturns, double riskFreeRate) {

        if (investmentReturns == null || investmentReturns.isEmpty()) {
            throw new IllegalArgumentException("Returns list cannot be null or empty");
        }

        // Convert risk-free rate from percentage to decimal
        double dailyRiskFreeRate = riskFreeRate / 100;

        double meanReturn = investmentReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        // Annualize the mean return (assuming daily returns)
        double annualizedReturn = meanReturn * 252;

        double volatility = calculateVolatility(investmentReturns);
        // Convert volatility back to decimal form since it's in percentage
        volatility = volatility / 100;

        // Annualize the daily risk-free rate
        double annualizedRiskFreeRate = dailyRiskFreeRate * 252;

        return (annualizedReturn - annualizedRiskFreeRate) / volatility;
    }

    /**
     * Calculate the beta of an investment.
     *
     * @param investmentReturns List of investment returns
     * @param marketReturns List of market benchmark returns
     * @return Beta
     */
    @Override
    public double calculateBeta(List<Double> investmentReturns, List<Double> marketReturns) {
        if (investmentReturns == null || marketReturns == null || investmentReturns.size() != marketReturns.size()) {
            throw new IllegalArgumentException("Returns lists must be non-null and of the same size");
        }

        // Calculate the means for investment and market returns
        double investmentMean = investmentReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double marketMean = marketReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Calculate covariance and market variance in a single loop for efficiency
        double covariance = 0.0;
        double marketVariance = 0.0;
        for (int i = 0; i < investmentReturns.size(); i++) {
            double investmentDiff = investmentReturns.get(i) - investmentMean;
            double marketDiff = marketReturns.get(i) - marketMean;

            covariance += investmentDiff * marketDiff;
            marketVariance += marketDiff * marketDiff;
        }

        covariance /= investmentReturns.size();
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
     * @param portfolioVolitalityWithCorrelation Standard deviation of portfolio returns
     * @param zScore               Z-score for the desired confidence level (e.g., 1.645 for 95%)
     * @return Value at Risk (VaR)
     */
    @Override
    public double calculateValueAtRisk(double meanReturn, double portfolioVolitalityWithCorrelation, double zScore) {
        return meanReturn - zScore * portfolioVolitalityWithCorrelation;
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
