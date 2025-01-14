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

@Component
@RequiredArgsConstructor
public class RiskMetricHelper {

    @Value("${alphavantage.api.url}")
    private static String RISK_FREE_RATE_API_URL;
    @Value("${alphavantage.api.key}")
    private static String RISK_FREE_RATE_API_KEY;
    private static final String MAKRET_SYMBOL = "SPY";
    @Value("${risk.confidenceLevel}")
    private double confidenceLevel;

    private final RiskMetricService riskMetricService;
    private final PortfolioService portfolioService;
    private final AssetService assetService;


    /**
     * Calculates the average investment returns for each asset in the given portfolio over the specified number of days.
     * The investment return is calculated as the average of daily returns over the specified period, excluding the first day.
     *
     * @param portfolio The portfolio containing the assets for which the returns are calculated.
     * @param days The number of days over which the returns should be calculated (e.g., 30 for the last 30 days).
     * @return A list of average investment returns for each asset in the portfolio, where each element corresponds to the
     *         average return for an asset.
     * @throws RuntimeException If fetching historical prices for an asset fails.
     */
    public List<Double> calculateInvestmentReturns(Portfolio portfolio, int days) {
        List<Double> investmentReturns = new ArrayList<>();

        List<Asset> assets = portfolio.getAssets();

        // Calculate the target historical date (30 days ago)
        LocalDate historicalDate = LocalDate.now().minusDays(days);

        for (Asset asset : assets) {
            try {
                // Fetch daily prices for the last 30 days. HAS BEEN TESTED. WORKS FINE!
                List<Double> dailyPrices = assetService.fetchPrices(asset, days);

                // Calculate daily returns for the last 30 days (excluding the first day)
                List<Double> dailyReturns = new ArrayList<>();
                for (int i = 1; i < dailyPrices.size(); i++) {
                    double logReturn = Math.log(dailyPrices.get(i) / dailyPrices.get(i - 1));
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
     * Fetches the risk-free rate based on the given interval and maturity using the Alpha Vantage API.
     *
     * @param interval The interval for the treasury yield data. Accepted values: "daily", "weekly", "monthly".
     * @param maturity The maturity period for the treasury yield. Accepted values: "3month", "2year", "5year", "10year", "30year".
     * @return The risk-free rate as a double value.
     * @throws RuntimeException If the data cannot be retrieved or parsed from the API.
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
                    return latestEntry.getDouble("value");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.0;
    }


    /**
     * Calculates the daily market returns for a given market symbol over the specified number of days.
     * The market return is calculated as the percentage change in price between consecutive days.
     *
     * @param marketSymbol The symbol of the market (e.g., "SPY" for S&P 500) for which the returns are calculated.
     * @param days The number of days over which the market returns should be calculated (e.g., 30 for the last 30 days).
     * @return A list of daily market returns, where each element represents the return for a particular day
     *         compared to the previous day.
     * @throws RuntimeException If fetching historical prices for the market fails.
     */
    public List<Double> calculateMarketReturns(String marketSymbol, int days) {

        List<Double> marketReturns = new ArrayList<>();

        Asset temp = new Asset();
        temp.setSymbol(marketSymbol);

        try {
            // Fetch daily prices for the last 30 days
            List<Double> dailyPrices = assetService.fetchPrices(temp, days);

            // Calculate daily returns for the last 30 days (excluding the first day)
            for (int i = 1; i < dailyPrices.size(); i++) {
                double logReturn = Math.log(dailyPrices.get(i) / dailyPrices.get(i - 1));
                marketReturns.add(logReturn);
            }

        } catch (Exception e) {
            // Log the error and skip the asset
            System.err.println("Error fetching price for asset " + temp.getSymbol() + ": " + e.getMessage());
        }

        return marketReturns;
    }


    /**
     * Calculates the average portfolio return over a specified number of days.
     * The portfolio return is calculated by first determining the daily returns for each asset
     * in the portfolio and then calculating the average of those daily returns.
     *
     * @param portfolio The portfolio whose return is to be calculated. It contains a list of assets.
     * @param days The number of days over which the portfolio return should be calculated (e.g., 30 for the last 30 days).
     * @return The average portfolio return over the specified number of days. The return is calculated
     *         as the average of the daily returns for all assets in the portfolio.
     *         If no returns are available, it returns 0.0.
     * @throws IllegalArgumentException If the portfolio is null or if the number of days is invalid.
     */
    public double calculatePortfolioReturn(Portfolio portfolio, int days) {

        List<Double> dailyReturns = calculateInvestmentReturns(portfolio, days);

        // Calculate the average daily return
        return dailyReturns.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculates the total value of a portfolio by summing the value of each asset in the portfolio.
     * The value of each asset is determined by multiplying its quantity by its current price.
     *
     * @param portfolio The portfolio whose total value is to be calculated. It contains a list of assets.
     * @return The total value of the portfolio, which is the sum of the values of all assets in the portfolio.
     *         If the portfolio contains no assets, it returns 0.0.
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
     * Calculates the average market return over a specified number of days for a given market symbol.
     * It fetches the daily returns for the market and calculates the average of these returns.
     *
     * @param marketSymbol The symbol of the market (e.g., "SPY" for the S&P 500) whose returns are to be calculated.
     * @param days The number of days over which to calculate the market return.
     *              The method will fetch the daily returns for the specified number of days and calculate the average.
     * @return The average daily return of the market over the specified number of days.
     *         If no market returns are available, it returns 0.0.
     */
    public double calculateMarketReturn(String marketSymbol, int days) {

        // Fetch daily returns for the market
        List<Double> dailyReturns = calculateMarketReturns(marketSymbol, days);

        // Calculate the average daily return
        return dailyReturns.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }


    /**
     * Calculates the mean return of a portfolio over two time periods (30 days and 60 days) for each asset
     * and computes the weighted average return based on the asset weights in the portfolio.
     *
     * The method fetches historical prices for each asset in the portfolio for the last 60 days, calculates
     * the 30-day and 60-day returns for each asset, and then computes the weighted mean return of the portfolio
     * based on the asset quantities and their respective prices.
     *
     * @param portfolio The portfolio whose mean return is to be calculated.
     *                  The portfolio contains a list of assets with their respective quantities and prices.
     * @return The weighted mean return of the portfolio based on 30-day and 60-day returns for each asset.
     *         If there is insufficient data for any asset, it will be skipped.
     */
    public double calculateMeanReturn(Portfolio portfolio) {
        List<Asset> assets = portfolio.getAssets();
        int numAssets = assets.size();

        // Fetch historical data for all assets
        Map<Asset, List<Double>> historicalPrices = new HashMap<>();
        for (Asset asset : assets) {
            List<Double> prices = assetService.fetchPrices(asset, 60); // Fetch 60 days of data
            historicalPrices.put(asset, prices);
        }

        // Calculate portfolio weights
        double totalValue = calculateTotalPortfolioValue(portfolio);
        Map<Asset, Double> weights = new HashMap<>();
        for (Asset asset : assets) {
            double weight = (asset.getQuantity() * asset.getCurrentPrice()) / totalValue;
            weights.put(asset, weight);
        }

        // Calculate weighted mean return across periods
        double portfolioMeanReturn = 0.0;
        for (Asset asset : assets) {
            List<Double> prices = historicalPrices.get(asset);

            if (prices.size() < 60) {
                System.err.println("Insufficient data for asset: " + asset.getSymbol());
                continue;
            }

            // Calculate 30-day and 60-day returns
            double return30Days = (prices.get(29) - prices.get(0)) / prices.get(0); // Adjust index if prices are reverse-ordered
            double return60Days = (prices.get(59) - prices.get(0)) / prices.get(0);

            // Average return for this asset
            double meanReturn = (return30Days + return60Days) / 2;

            // Add weighted return to portfolio mean
            portfolioMeanReturn += weights.get(asset) * meanReturn;
        }

        return portfolioMeanReturn;
    }


    /**
     * Calculates the volatility (standard deviation) of a portfolio based on the historical returns
     * of its assets. The volatility is computed by first calculating the daily returns for each asset,
     * then computing the portfolio's total variance by considering asset weights and the covariance
     * between asset returns. The method then returns the portfolio's overall volatility as the square root
     * of the variance.
     *
     * The steps involved include:
     * 1. Fetching historical returns for each asset in the portfolio.
     * 2. Calculating the asset weights based on their current value in the portfolio.
     * 3. Computing the standard deviation (volatility) for each asset.
     * 4. Calculating the covariance matrix of asset returns.
     * 5. Computing the portfolio's variance using the covariance matrix and asset weights.
     * 6. Returning the portfolio's volatility as the square root of the variance.
     *
     * @param portfolio The portfolio for which the volatility is to be calculated. It contains a list of assets
     *                  with their quantities and prices.
     * @param days The number of days of historical price data to use when calculating the returns and volatility.
     * @return The volatility (standard deviation) of the portfolio, calculated based on historical returns of its assets.
     * @throws IllegalArgumentException if the portfolio contains no assets.
     */
    public double calculatePortfolioVolatilityWithCorrelation(Portfolio portfolio, int days) {
        List<Asset> assets = portfolio.getAssets();
        int numAssets = assets.size();

        if (numAssets == 0) {
            throw new IllegalArgumentException("Portfolio must contain at least one asset.");
        }

        // Step 1: Fetch historical returns for all assets
        Map<Asset, List<Double>> assetReturns = new HashMap<>();
        for (Asset asset : assets) {
            List<Double> dailyPrices = assetService.fetchPrices(asset, days);
            List<Double> dailyReturns = new ArrayList<>();
            for (int i = 1; i < dailyPrices.size(); i++) {
                double dailyReturn = (dailyPrices.get(i) - dailyPrices.get(i - 1)) / dailyPrices.get(i - 1);
                dailyReturns.add(dailyReturn);
            }
            assetReturns.put(asset, dailyReturns);
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
        return Math.sqrt(portfolioVariance);
    }


    /**
     * Retrieves the z-score corresponding to a given confidence level.
     *
     * The z-score is a statistical measurement that represents the number of
     * standard deviations a data point is from the mean. This method maps
     * common confidence levels (e.g., 90%, 95%, 99%) to their respective z-scores.
     *
     * @return the z-score associated with the given confidence level
     * @throws IllegalArgumentException if the confidence level is unsupported
     */
    public double getZScoreFromConfidenceLevel() {

        if (confidenceLevel == 0.90) return 1.28;
        if (confidenceLevel == 0.95) return 1.645;
        if (confidenceLevel == 0.99) return 2.33;
        throw new IllegalArgumentException("Unsupported confidence level: " + confidenceLevel);
    }


    /**
     * Calculates the portfolio values for each day over a specified period. The method considers the value of each asset
     * in the portfolio, based on their daily prices, and computes the weighted contribution of each asset to the overall portfolio value.
     *
     * The portfolio value for each day is determined by the weighted sum of the asset prices, where the weight of each asset
     * is calculated based on its proportion to the total portfolio value. This provides a daily snapshot of how the portfolio's
     * value changes over the given time period.
     *
     * @param portfolio The portfolio containing the assets whose values are to be calculated.
     * @param days The number of days over which to calculate the portfolio values.
     * @return A list of portfolio values for each day over the specified period.
     * @throws IllegalArgumentException if the portfolio is empty or contains assets with insufficient data.
     */
    public List<Double> calculatePortfolioValues(Portfolio portfolio, int days) {
        List<Double> portfolioValues = new ArrayList<>();

        // Calculate the total portfolio value once, as it's constant throughout
        double totalPortfolioValue = calculateTotalPortfolioValue(portfolio);

        // Map to store daily prices for each asset
        Map<Asset, List<Double>> assetValues = new HashMap<>();

        // Loop through each asset in the portfolio and fetch their daily prices
        for (Asset asset : portfolio.getAssets()) {
            List<Double> dailyPrices = assetService.fetchPrices(asset, days);
            assetValues.put(asset, dailyPrices);
        }

        // Loop through each day (up to 'days')
        for (int i = 0; i < days; i++) {
            double dailyPortfolioValue = 0.0;

            // Loop through each asset and calculate its weighted value on this day
            for (Asset asset : portfolio.getAssets()) {
                List<Double> dailyPrices = assetValues.get(asset);
                if (dailyPrices != null && dailyPrices.size() > i) {
                    double dailyPrice = dailyPrices.get(i);  // Price of the asset on day 'i'
                    double assetWeight = (asset.getQuantity() * dailyPrice) / totalPortfolioValue;

                    // Add the weighted asset value to the portfolio value for this day
                    dailyPortfolioValue += dailyPrice * assetWeight;
                }
            }

            // Add the portfolio value for the current day to the list
            portfolioValues.add(dailyPortfolioValue);
        }

        return portfolioValues;
    }


}
