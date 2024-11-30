package com.erenkalkan.financial_risk_analysis.util;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.service.AssetService;
import com.erenkalkan.financial_risk_analysis.service.HistoricalDataService;
import com.erenkalkan.financial_risk_analysis.service.PortfolioService;
import com.erenkalkan.financial_risk_analysis.service.RiskMetricService;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
    private final HistoricalDataService historicalDataService;
    private final AssetService assetService;


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
     * Calculates the return on the portfolio by considering the individual returns of assets,
     * weighted by their proportion in the portfolio.
     * <p>
     * This method iterates through each asset in the portfolio, calculates the individual asset's
     * return, applies the weight of the asset in the portfolio, and accumulates the weighted returns
     * to compute the overall portfolio return.
     *
     * @param portfolio The portfolio for which the return is being calculated.
     * @return The weighted return of the portfolio as a decimal value (e.g., 0.05 for a 5% return).
     */
    public double findPortfolioReturn(Portfolio portfolio) {

        double portfolioReturn = 0.0;

        double totalPortfolioValue = calculateTotalPortfolioValue(portfolio);

        for (Asset asset : portfolio.getAssets()) {
            Double purchasePrice = asset.getPurchasePrice();
            Double currentPrice = asset.getCurrentPrice();

            double assetWeight = (asset.getQuantity() * currentPrice) / totalPortfolioValue;

            if (purchasePrice != null && currentPrice != null) {
                double investmentReturn = (currentPrice - purchasePrice) / purchasePrice;
                double weightedInvestmentReturn = investmentReturn * assetWeight;
                portfolioReturn += weightedInvestmentReturn;
            } else {
                portfolioReturn += 0.0;
            }
        }

        return portfolioReturn;
    }

    private double calculateTotalPortfolioValue(Portfolio portfolio) {

        List<Asset> assets = portfolio.getAssets();
        double totalValue = 0.0;
        for (Asset asset : assets) {
            totalValue += asset.getQuantity() * asset.getCurrentPrice();
        }
        return totalValue;
    }


    /**
     * Calculates the total value of a portfolio based on the price of each asset
     * from a specified number of days ago.
     *
     * The method fetches the historical price for each asset in the portfolio at
     * the target date (calculated as the current date minus the specified number of days),
     * and then calculates the total value of the portfolio by summing the weighted
     * values of each asset (quantity * historical price).
     *
     * @param portfolio the portfolio for which the total value is being calculated
     * @param days the number of days ago from the current date to fetch historical asset prices
     * @return the total value of the portfolio based on historical prices from the specified date
     * @throws RuntimeException if fetching prices for any asset fails or returns invalid data
     */
    public double calculateTotalPortfolioValueLastXDays(Portfolio portfolio, int days) {

        List<Asset> assets = portfolio.getAssets();
        double totalValue = 0.0;

        // Calculate the target historical date
        LocalDate historicalDate = LocalDate.now().minusDays(days);

        for (Asset asset : assets) {
            try {
                // Fetch historical price for the asset
                Asset tempAsset = new Asset();
                tempAsset.setSymbol(asset.getSymbol());
                tempAsset.setPurchaseDate(historicalDate);

                List<Double> prices = assetService.fetchPrices(tempAsset);

                // Validate the fetched data
                if (prices == null || prices.isEmpty()) {
                    throw new RuntimeException("Failed to fetch historical prices for asset: " + asset.getSymbol());
                }

                double lastXDaysPrice = prices.get(0); // Price at historical date
                totalValue += asset.getQuantity() * lastXDaysPrice;

            } catch (Exception e) {
                // Log the error and skip the asset
                System.err.println("Error fetching price for asset " + asset.getSymbol() + ": " + e.getMessage());
            }
        }
        return totalValue;
    }


    public List<Double> calculateInvestmentReturns(Portfolio portfolio, int days) {
        List<Double> investmentReturns = new ArrayList<>();

        List<Asset> assets = portfolio.getAssets();

        // Calculate the target historical date (30 days ago)
        LocalDate historicalDate = LocalDate.now().minusDays(days);

        for (Asset asset : assets) {
            try {
                // Fetch daily prices for the last 30 days
                List<Double> dailyPrices = assetService.fetchPrices(asset, days);

                // Calculate daily returns for the last 30 days (excluding the first day)
                List<Double> dailyReturns = new ArrayList<>();
                for (int i = 1; i < dailyPrices.size(); i++) {
                    double dailyReturn = (dailyPrices.get(i) - dailyPrices.get(i - 1)) / dailyPrices.get(i - 1);
                    dailyReturns.add(dailyReturn);
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

    public List<Double> calculateMarketReturns(String marketSymbol, int days) {

        List<Double> marketReturns = new ArrayList<>();

        Asset temp = new Asset();
        temp.setSymbol(marketSymbol);

            try {
                // Fetch daily prices for the last 30 days
                List<Double> dailyPrices = assetService.fetchPrices(temp, days);

                // Calculate daily returns for the last 30 days (excluding the first day)
                for (int i = 1; i < dailyPrices.size(); i++) {
                    double dailyReturn = (dailyPrices.get(i) - dailyPrices.get(i - 1)) / dailyPrices.get(i - 1);
                    marketReturns.add(dailyReturn);
                }

            } catch (Exception e) {
                // Log the error and skip the asset
                System.err.println("Error fetching price for asset " + temp.getSymbol() + ": " + e.getMessage());
            }

            return marketReturns;
    }



    /**
     * Calculates the return on the market index (e.g., S&P 500) based on the earliest purchase date
     * of the assets in the portfolio and the current market price.
     * <p>
     * This method fetches the historical market data (e.g., from Alpha Vantage) using the earliest
     * purchase date in the portfolio as the start date, and calculates the market return from the
     * purchase date to the current date.
     *
     * @param portfolio The portfolio whose earliest purchase date is used to determine the market
     *                  return period.
     * @return The return of the market (e.g., S&P 500) as a decimal value (e.g., 0.05 for a 5% return).
     */
    public double calculateMarketReturn(Portfolio portfolio) {

        LocalDate earliestPurchaseDate = findEarliestPurchaseDate(portfolio);

        Asset asset = new Asset();
        asset.setSymbol("SPY");
        asset.setPurchaseDate(earliestPurchaseDate);

        List<Double> prices = assetService.fetchPrices(asset);

        // Price at purchase date is being stored first in the list, the current price after

        double marketPriceAtPurchaseDate = prices.get(0);
        double currentMarketPrice = prices.get(1);

        return (currentMarketPrice - marketPriceAtPurchaseDate) / marketPriceAtPurchaseDate;
    }

    private LocalDate findEarliestPurchaseDate(Portfolio portfolio) {
        LocalDate earliestDate = LocalDate.now();
        for (int i = 0; i < portfolio.getAssets().size(); i++) {
            if (portfolio.getAssets().get(i).getPurchaseDate().isBefore(earliestDate)) {
                earliestDate = portfolio.getAssets().get(i).getPurchaseDate();
            }
        }
        return earliestDate;
    }


    public double calculateHistoricalVolatility(Portfolio portfolio) {

        // Step 1: Get the list of assets
        List<Asset> assets = portfolio.getAssets();
        int numAssets = assets.size();

        // Step 2: Calculate asset returns (e.g., daily returns for the last 30 days)
        Map<Asset, List<Double>> assetReturns = new HashMap<>();
        for (Asset asset : assets) {
            Asset ReturnLast30Days = new Asset();
            Asset ReturnLast60Days = new Asset();
            ReturnLast30Days.setSymbol(asset.getSymbol());
            ReturnLast60Days.setSymbol(asset.getSymbol());
            ReturnLast30Days.setPurchaseDate(LocalDate.now().minusDays(30));
            ReturnLast60Days.setPurchaseDate(LocalDate.now().minusDays(60));

            double pastPrice30Days = assetService.fetchPrices(ReturnLast30Days).get(0);
            double currentPrice30Days = assetService.fetchPrices(ReturnLast60Days).get(1);
            double return30Days =  (currentPrice30Days -pastPrice30Days)/ pastPrice30Days;

            double pastPrice60Days = assetService.fetchPrices(ReturnLast60Days).get(0);
            double currentPrice60Days = assetService.fetchPrices(ReturnLast60Days).get(1);
            double return60Days =  (currentPrice60Days -pastPrice60Days)/ pastPrice60Days;

            List<Double> returns = new ArrayList<>();
            returns.add(return30Days);
            returns.add(return60Days);

            assetReturns.put(asset, returns);
        }

        // Step 3: Calculate covariance matrix between asset returns
        double[][] covarianceMatrix = new double[numAssets][numAssets];
        for (int i = 0; i < numAssets; i++) {
            for (int j = 0; j < numAssets; j++) {
                covarianceMatrix[i][j] = calculateCovariance(assetReturns.get(assets.get(i)), assetReturns.get(assets.get(j)));
            }
        }

        // Step 4: Calculate portfolio weights
        double[] weights = new double[numAssets];
        double totalValue = calculateTotalPortfolioValue(portfolio);
        for (int i = 0; i < numAssets; i++) {
            weights[i] = (assets.get(i).getQuantity() * assets.get(i).getCurrentPrice()) / totalValue;
        }

        // Step 5: Calculate portfolio volatility using the formula
        double portfolioVolatility = 0.0;
        for (int i = 0; i < numAssets; i++) {
            for (int j = 0; j < numAssets; j++) {
                portfolioVolatility += weights[i] * weights[j] * covarianceMatrix[i][j];
            }
        }

        // Step 6: Return the portfolio volatility
        return Math.sqrt(portfolioVolatility);
    }

    public double calculateCovariance(List<Double> returnsX, List<Double> returnsY) {
        if (returnsX == null || returnsY == null || returnsX.size() != returnsY.size()) {
            throw new IllegalArgumentException("Return lists must be non-null and of the same size");
        }

        int n = returnsX.size();
        double meanX = returnsX.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanY = returnsY.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double covariance = 0.0;
        for (int i = 0; i < n; i++) {
            covariance += (returnsX.get(i) - meanX) * (returnsY.get(i) - meanY);
        }

        // Return the average covariance (dividing by n-1 for sample covariance)
        return covariance / (n - 1);
    }


    /**
     * Calculates the historical mean return of a portfolio based on the weighted average returns
     * of its constituent assets. Each asset's historical returns are calculated over multiple
     * periods (e.g., last 30 days and last 60 days), and the overall portfolio mean return is
     * derived by weighting these returns by the proportion of the asset's value in the portfolio.
     *
     * <p>This method uses the following steps:
     * <ol>
     *   <li>Fetches historical price data for each asset over specified time periods (30 and 60 days).</li>
     *   <li>Calculates the returns for these periods.</li>
     *   <li>Computes the portfolio weights for each asset based on their proportion of the total portfolio value.</li>
     *   <li>Determines the mean return for each asset by averaging its returns over the periods.</li>
     *   <li>Calculates the weighted average of all asset mean returns to obtain the portfolio's mean return.</li>
     * </ol>
     *
     * @param portfolio The portfolio containing a list of assets for which the mean return is calculated.
     *                  Each asset must have information about its current price, quantity, and symbol.
     * @return The historical mean return of the portfolio as a double, representing the weighted average
     *         return of its assets over the specified periods.
     * @throws IllegalArgumentException If the portfolio is empty or if any required data (e.g., prices) is missing.
     * @throws RuntimeException If the asset service fails to fetch historical price data.
     */
    public double calculateMeanReturn(Portfolio portfolio) {
        List<Asset> assets = portfolio.getAssets();
        int numAssets = assets.size();

        Map<Asset, List<Double>> assetReturns = new HashMap<>();
        for (Asset asset : assets) {
            Asset ReturnLast30Days = new Asset();
            Asset ReturnLast60Days = new Asset();
            ReturnLast30Days.setSymbol(asset.getSymbol());
            ReturnLast60Days.setSymbol(asset.getSymbol());
            if(asset.getPurchaseDate().isBefore(LocalDate.now().minusDays(30))){
                ReturnLast30Days.setPurchaseDate(LocalDate.now().minusDays(30));
                ReturnLast60Days.setPurchaseDate(LocalDate.now().minusDays(60));
            }

            // Fetch prices for the 30-day and 60-day periods
            double pastPrice30Days = assetService.fetchPrices(ReturnLast30Days).get(0);
            double currentPrice30Days = assetService.fetchPrices(ReturnLast30Days).get(1);
            double return30Days = (currentPrice30Days - pastPrice30Days) / pastPrice30Days;

            double pastPrice60Days = assetService.fetchPrices(ReturnLast60Days).get(0);
            double currentPrice60Days = assetService.fetchPrices(ReturnLast60Days).get(1);
            double return60Days = (currentPrice60Days - pastPrice60Days) / pastPrice60Days;

            List<Double> returns = new ArrayList<>();
            returns.add(return30Days);
            returns.add(return60Days);

            assetReturns.put(asset, returns);
        }

        // Calculate portfolio weights
        double[] weights = new double[numAssets];
        double totalValue = calculateTotalPortfolioValue(portfolio);
        for (int i = 0; i < numAssets; i++) {
            weights[i] = (assets.get(i).getQuantity() * assets.get(i).getCurrentPrice()) / totalValue;
        }

        // Calculate weighted mean return across multiple periods
        double portfolioMeanReturn = 0.0;
        for (int i = 0; i < numAssets; i++) {
            List<Double> returns = assetReturns.get(assets.get(i));
            double averageReturnForAsset = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            portfolioMeanReturn += weights[i] * averageReturnForAsset;
        }

        return portfolioMeanReturn;
    }


    /**
     * Retrieves the z-score corresponding to a given confidence level.
     *
     * The z-score is a statistical measurement that represents the number of
     * standard deviations a data point is from the mean. This method maps
     * common confidence levels (e.g., 90%, 95%, 99%) to their respective z-scores.
     *
     * @param confidenceLevel the confidence level (e.g., 0.90 for 90%)
     * @return the z-score associated with the given confidence level
     * @throws IllegalArgumentException if the confidence level is unsupported
     */
    private double getZScoreFromConfidenceLevel(double confidenceLevel) {

        if (confidenceLevel == 0.90) return 1.28;
        if (confidenceLevel == 0.95) return 1.645;
        if (confidenceLevel == 0.99) return 2.33;
        throw new IllegalArgumentException("Unsupported confidence level: " + confidenceLevel);
    }





}
