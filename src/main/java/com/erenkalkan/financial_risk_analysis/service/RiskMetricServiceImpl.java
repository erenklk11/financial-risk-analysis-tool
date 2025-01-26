package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.RiskMetric;
import com.erenkalkan.financial_risk_analysis.repository.RiskMetricRepository;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.transaction.Transactional;
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
    @Transactional
    public void save(RiskMetric riskMetric) {
        riskMetricRepository.save(riskMetric);
    }

    @Override
    @Transactional
    public void deleteByPortfolio(Portfolio portfolio) {
        riskMetricRepository.deleteByPortfolio(portfolio);
    }


    /**
     * Calculates the Sharpe ratio for a given list of investment returns.
     *
     * @param investmentReturns  A list of daily investment returns.
     * @param riskFreeRate       The annual risk-free rate as a percentage.
     * @param portfolioVolatility The annual portfolio volatility.
     * @return The Sharpe ratio.
     * @throws IllegalArgumentException if the returns list is null or empty.
     */
    @Override
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
     * Calculates the portfolio beta based on the asset returns and market returns.
     *
     * @param portfolio          The portfolio containing the assets.
     * @param assetReturns       A map of assets to their respective list of returns.
     * @param marketReturns      A list of market returns.
     * @return The portfolio beta.
     * @throws IllegalArgumentException if the returns lists are null or of different sizes.
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

    /**
     * Calculates the beta of an asset based on its returns and the market returns.
     *
     * @param assetReturns  A list of asset returns.
     * @param marketReturns A list of market returns.
     * @return The asset beta.
     * @throws IllegalArgumentException if the returns lists are null or of different sizes.
     */
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
     * Calculates the alpha of the portfolio.
     *
     * @param portfolioReturn    The daily portfolio return.
     * @param marketReturn       The daily market return.
     * @param riskFreeRate       The annual risk-free rate as a percentage.
     * @param beta               The portfolio beta.
     * @return The alpha of the portfolio.
     */
    @Override
    public double calculateAlpha(double portfolioReturn, double marketReturn, double riskFreeRate, double beta) {

        return portfolioReturn - (riskFreeRate + beta * (marketReturn - riskFreeRate));
    }


    /**
     * Calculates the Value at Risk (VaR) of the portfolio.
     *
     * @param meanReturn                     The mean daily return of the portfolio.
     * @param portfolioVolatilityWithCorrelation The annualized portfolio volatility with correlation.
     * @param zScore                         The z-score corresponding to the confidence level.
     * @return The Value at Risk (VaR).
     */
    @Override
    public double calculateValueAtRisk(double meanReturn, double portfolioVolatilityWithCorrelation, double zScore) {

        // Annualize the mean return and volatility
        double annualizedMeanReturn = meanReturn * 252;

        return annualizedMeanReturn - zScore * portfolioVolatilityWithCorrelation;
    }


    /**
     * Calculates the maximum drawdown of the portfolio.
     *
     * @param portfolioValues    A list of portfolio values over time.
     * @return The maximum drawdown as a percentage.
     * @throws IllegalArgumentException if the portfolio values list is null or empty.
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
