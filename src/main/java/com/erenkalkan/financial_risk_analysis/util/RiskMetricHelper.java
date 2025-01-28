package com.erenkalkan.financial_risk_analysis.util;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.service.AssetService;
import com.erenkalkan.financial_risk_analysis.service.PortfolioService;
import com.erenkalkan.financial_risk_analysis.service.RiskMetricService;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RiskMetricHelper {

    @Value("${alphavantage.api.url}")
    private String RISK_FREE_RATE_API_URL;
    @Value("${alphavantage.api.key}")
    private String RISK_FREE_RATE_API_KEY;
    @Value("${risk.confidenceLevel}")
    private double confidenceLevel;


    private final AssetService assetService;


    /**
     * Calculates the investment returns for a given portfolio based on historical prices.
     *
     * @param portfolio         The portfolio containing the assets.
     * @param historicalPrices  A map of assets to their respective list of historical prices.
     * @return A list of investment returns.
     */
    public List<Double> calculateInvestmentReturns(Portfolio portfolio, Map<Asset, List<Double>> historicalPrices) {
        List<Double> investmentReturns = new ArrayList<>();

        List<Asset> assets = portfolio.getAssets();

        for (Asset asset : assets) {
            try {
                List<Double> dailyPrices = historicalPrices.get(asset);

                // Calculate daily logarithmic returns for the asset
                List<Double> dailyReturns = new ArrayList<>();
                for (int i = 1; i < dailyPrices.size(); i++) {
                    double logReturn = Math.log(dailyPrices.get(i-1) / dailyPrices.get(i));
                    dailyReturns.add(logReturn);
                }

                // Calculate the average of daily returns for the asset
                double meanReturn = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                investmentReturns.add(meanReturn); // Add to the investment returns list

            } catch (Exception e) {
                // Log the error and skip the asset
                System.err.println("Error fetching price for asset " + asset.getSymbol() + ": " + e.getMessage());
            }
        }

        return investmentReturns;
    }


    /**
     * Calculates the daily returns for a given asset based on historical prices.
     *
     * @param asset             The asset for which to calculate returns.
     * @param historicalPrices  A map of assets to their respective list of historical prices.
     * @return A list of daily returns for the asset.
     */
    public List<Double> calculateAssetReturns(Asset asset, Map<Asset, List<Double>> historicalPrices) {

        List<Double> assetReturns = new ArrayList<>();

        try {
            // Fetch daily prices for the last trading year (252 days)
            List<Double> dailyPrices = historicalPrices.get(asset);

            // Calculate daily logarithmic returns for the asset
            for (int i = 1; i < dailyPrices.size(); i++) {
                double logReturn = Math.log(dailyPrices.get(i-1) / dailyPrices.get(i));
                assetReturns.add(logReturn);
            }

        } catch (Exception e) {
            // Log the error and skip the asset
            System.err.println("Error fetching price for asset " + ": " + e.getMessage());
        }

        return assetReturns;
    }


    /**
     * Fetches the risk-free rate from an external API.
     *
     * @param interval  The interval for the risk-free rate (e.g., daily, monthly).
     * @param maturity  The maturity period for the risk-free rate (e.g., 1 month, 1 year).
     * @return The risk-free rate as a percentage.
     */
    public double getRiskFreeRate(String interval, String maturity) {

        try {
            String uri = String.format(
                    "%s?function=TREASURY_YIELD&interval=%s&maturity=%s&apikey=%s",
                    RISK_FREE_RATE_API_URL, interval, maturity, RISK_FREE_RATE_API_KEY
            );

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject jsonObject = new JSONObject(response.body());

            if (jsonObject.has("data")) {
                JSONArray dataArray = jsonObject.getJSONArray("data");
                if (!dataArray.isEmpty()) {
                    JSONObject latestEntry = dataArray.getJSONObject(0);
                    System.out.println("Risk free rate found: " + latestEntry.getDouble("value"));
                    return latestEntry.getDouble("value");
                }
            }
        } catch (Exception e) {
            System.out.println("Risk Free rate error" + e.getMessage());;
        }

        return 0.0;
    }


    /**
     * Calculates the market returns based on historical prices.
     *
     * @param historicalPrices  A list of historical prices for the market.
     * @return A list of market returns.
     */
    public List<Double> calculateMarketReturns(List<Double> historicalPrices) {

        List<Double> marketReturns = new ArrayList<>();

        try {
            // Fetch daily prices for the last trading year (252 days)
            List<Double> dailyPrices = historicalPrices;

            // Calculate daily logarithmic returns for the asset
            for (int i = 1; i < dailyPrices.size(); i++) {
                double logReturn = Math.log(dailyPrices.get(i-1) / dailyPrices.get(i));
                marketReturns.add(logReturn);
            }

        } catch (Exception e) {
            // Log the error and skip the asset
            System.err.println("Error fetching price for SPY "  + ": " + e.getMessage());
        }

        return marketReturns;
    }


    /**
     * Calculates the annualized return of a portfolio based on daily returns.
     *
     * @param returns  A list of daily returns for the portfolio.
     * @return The annualized return of the portfolio.
     */
    public double calculatePortfolioReturn(List<Double> returns) {

        // Sum log returns to calculate the log cumulative return
        double logCumulativeReturn = returns.stream().mapToDouble(Double::doubleValue).sum();

        // Annualize the log return (assuming 252 trading days)
        return Math.exp(logCumulativeReturn * (252.0 / returns.size())) - 1;
    }


    /**
     * Calculates the total value of a portfolio based on the current prices of its assets.
     *
     * @param portfolio  The portfolio containing the assets.
     * @return The total value of the portfolio.
     */
    public double calculateTotalPortfolioValue(Portfolio portfolio) {

        List<Asset> assets = portfolio.getAssets();
        double totalValue = 0.0;
        for (Asset asset : assets) {
            totalValue += asset.getQuantity() * asset.getCurrentPrice();
        }
        return totalValue;
    }


    /**
     * Calculates the annualized return of the market based on daily returns.
     *
     * @param returns  A list of daily returns for the market.
     * @return The annualized return of the market.
     */
    public double calculateMarketReturn(List<Double> returns) {

        // Sum log returns to calculate the log cumulative return
        double logCumulativeReturn = returns.stream().mapToDouble(Double::doubleValue).sum();

        // Annualize the log return (assuming 252 trading days)
        return Math.exp(logCumulativeReturn * (252.0 / returns.size())) - 1;
    }


    /**
     * Calculates the mean return of a portfolio based on historical prices.
     *
     * @param portfolio         The portfolio containing the assets.
     * @param historicalPrices  A map of assets to their respective list of historical prices.
     * @return The mean return of the portfolio.
     */
    public double calculateMeanReturn(Portfolio portfolio, Map<Asset, List<Double>> historicalPrices) {
        List<Asset> assets = portfolio.getAssets();

        // Calculate portfolio weights
        double totalValue = calculateTotalPortfolioValue(portfolio);
        Map<Asset, Double> weights = new HashMap<>();
        for (Asset asset : assets) {
            double weight = (asset.getQuantity() * asset.getCurrentPrice()) / totalValue;
            weights.put(asset, weight);
        }

        // Calculate daily returns and weighted mean return
        double portfolioMeanReturn = 0.0;
        for (Asset asset : assets) {
            List<Double> prices = historicalPrices.get(asset);
            List<Double> returns = new ArrayList<>();

            for (int i = 1; i < prices.size(); i++) {
                double dailyReturn = (prices.get(i) - prices.get(i-1)) / prices.get(i-1);
                returns.add(dailyReturn);
            }

            double meanReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            portfolioMeanReturn += weights.get(asset) * meanReturn;
        }

        return portfolioMeanReturn;
    }


    /**
     * Calculates the portfolio volatility with correlation based on historical returns.
     *
     * @param portfolio     The portfolio containing the assets.
     * @param assetReturns  A map of assets to their respective list of returns.
     * @return The portfolio volatility with correlation.
     */
    public double calculatePortfolioVolatilityWithCorrelation(Portfolio portfolio, Map<Asset, List<Double>> assetReturns) {
        List<Asset> assets = portfolio.getAssets();
        int numAssets = assets.size();

        if (numAssets == 0) {
            throw new IllegalArgumentException("Portfolio must contain at least one asset.");
        }


        // Step 2: Calculate asset weights
        double totalValue = calculateTotalPortfolioValue(portfolio);
        Map<Asset, Double> weights = new HashMap<>();
        for (Asset asset : assets) {
            double weight = (asset.getQuantity() * asset.getCurrentPrice()) / totalValue;
            weights.put(asset, weight);
        }

        // Step 3: Compute standard deviations (volatilities) for each asset
        Map<Asset, Double> volatilities = new HashMap<>();
        for (Asset asset : assets) {
            List<Double> returns = assetReturns.get(asset);

            if (returns.isEmpty()) {
                System.err.println("Insufficient data for asset: " + asset.getSymbol());
                continue;
            }

            double meanReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = returns.stream()
                    .mapToDouble(r -> Math.pow(r - meanReturn, 2))
                    .average()
                    .orElse(0.0);
            volatilities.put(asset, Math.sqrt(variance));
        }

        // Step 4: Compute the covariance matrix
        double[][] covarianceMatrix = new double[numAssets][numAssets];
        for (int i = 0; i < numAssets; i++) {
            for (int j = 0; j < numAssets; j++) {
                Asset assetI = assets.get(i);
                Asset assetJ = assets.get(j);

                List<Double> returnsI = assetReturns.get(assetI);
                List<Double> returnsJ = assetReturns.get(assetJ);

                if (returnsI.isEmpty() || returnsJ.isEmpty()) {
                    covarianceMatrix[i][j] = 0.0; // Insufficient data for covariance
                    continue;
                }

                double meanReturnI = returnsI.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double meanReturnJ = returnsJ.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                double covariance = 0.0;
                for (int k = 0; k < Math.min(returnsI.size(), returnsJ.size()); k++) {
                    covariance += (returnsI.get(k) - meanReturnI) * (returnsJ.get(k) - meanReturnJ);
                }
                covariance /= returnsI.size();

                covarianceMatrix[i][j] = covariance;
            }
        }

        // Step 5: Calculate portfolio variance
        double portfolioVariance = 0.0;
        for (int i = 0; i < numAssets; i++) {
            for (int j = 0; j < numAssets; j++) {
                Asset assetI = assets.get(i);
                Asset assetJ = assets.get(j);

                double weightI = weights.getOrDefault(assetI, 0.0);
                double weightJ = weights.getOrDefault(assetJ, 0.0);

                portfolioVariance += weightI * weightJ * covarianceMatrix[i][j];
            }
        }

        // Step 6: Return the square root of the variance (volatility)
        return Math.sqrt(portfolioVariance)* Math.sqrt(252);
    }


    /**
     * Gets the z-score corresponding to the configured confidence level.
     *
     * @return The z-score.
     * @throws IllegalArgumentException if the confidence level is unsupported.
     */
    public double getZScoreFromConfidenceLevel() {

        if (confidenceLevel == 0.90) return 1.28;
        if (confidenceLevel == 0.95) return 1.645;
        if (confidenceLevel == 0.99) return 2.33;
        throw new IllegalArgumentException("Unsupported confidence level: " + confidenceLevel);
    }


    /**
     * Calculates the portfolio values over a specified number of days based on historical prices.
     *
     * @param portfolio         The portfolio containing the assets.
     * @param historicalPrices  A map of assets to their respective list of historical prices.
     * @param days              The number of days for which to calculate portfolio values.
     * @return A list of portfolio values over the specified number of days.
     */
    public List<Double> calculatePortfolioValues(Portfolio portfolio, Map<Asset, List<Double>> historicalPrices, int days) {
        List<Double> portfolioValues = new ArrayList<>();

        // Get current quantities of each asset
        Map<Asset, Double> quantities = new HashMap<>();
        for (Asset asset : portfolio.getAssets()) {
            quantities.put(asset, asset.getQuantity());
        }


        // Calculate portfolio value for each day
        for (int i = 0; i < days; i++) {
            double dailyPortfolioValue = 0.0;

            // Sum up the value of each asset using fixed quantities
            for (Asset asset : portfolio.getAssets()) {
                List<Double> prices = historicalPrices.get(asset);
                if (prices != null && prices.size() > i) {
                    double price = prices.get(i);
                    double quantity = quantities.get(asset);
                    dailyPortfolioValue += price * quantity;
                }
            }

            portfolioValues.add(dailyPortfolioValue);
        }

        return portfolioValues;
    }

}
