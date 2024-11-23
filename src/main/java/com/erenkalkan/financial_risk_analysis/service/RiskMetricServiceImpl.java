package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.RiskMetric;
import com.erenkalkan.financial_risk_analysis.repository.RiskMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskMetricServiceImpl implements RiskMetricService{

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
     * @param returns List of investment returns
     * @return Standard deviation
     */
    @Override
    public double calculateVolatility(List<Double> returns) {

        if (returns == null || returns.isEmpty()) {
            throw new IllegalArgumentException("Returns list cannot be null or empty");
        }

        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double variance = returns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    /**
     * Calculate the Sharpe ratio.
     *
     * @param returns List of investment returns
     * @param riskFreeRate Risk-free rate of return
     * @return Sharpe ratio
     */
    @Override
    public double calculateSharpeRatio(List<Double> returns, double riskFreeRate) {
        if (returns == null || returns.isEmpty()) {
            throw new IllegalArgumentException("Returns list cannot be null or empty");
        }

        double averageReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double standardDeviation = this.calculateVolatility(returns);

        if (standardDeviation == 0) {
            throw new IllegalArgumentException("Standard deviation cannot be zero");
        }

        return (averageReturn - riskFreeRate) / standardDeviation;
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

        double investmentMean = investmentReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double marketMean = marketReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double covariance = 0.0;
        for (int i = 0; i < investmentReturns.size(); i++) {
            covariance += (investmentReturns.get(i) - investmentMean) * (marketReturns.get(i) - marketMean);
        }
        covariance /= investmentReturns.size();

        double marketVariance = marketReturns.stream()
                .mapToDouble(r -> Math.pow(r - marketMean, 2))
                .average()
                .orElse(0.0);

        if (marketVariance == 0) {
            throw new IllegalArgumentException("Market variance cannot be zero");
        }

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
        return portfolioReturn - (riskFreeRate + beta * (marketReturn - riskFreeRate));
    }

    /**
     * Calculate Value at Risk (VaR).
     *
     * @param mean Mean return of the portfolio
     * @param stdDev Standard deviation of portfolio returns
     * @param zScore Z-score for the desired confidence level (e.g., 1.645 for 95%)
     * @return Value at Risk (VaR)
     */
    @Override
    public double calculateValueAtRisk(double mean, double stdDev, double zScore) {
        return mean - zScore * stdDev;
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
