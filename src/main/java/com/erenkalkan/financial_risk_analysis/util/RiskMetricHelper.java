package com.erenkalkan.financial_risk_analysis.util;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.service.AssetService;
import com.erenkalkan.financial_risk_analysis.service.HistoricalDataService;
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class RiskMetricHelper {

    @Value("${alphavantage.api.url}")
    private static String RISK_FREE_RATE_API_URL;
    @Value("${alphavantage.api.key}")
    private static String RISK_FREE_RATE_API_KEY;
    private static final String MAKRET_SYMBOL = "SPY";

    private final RiskMetricService riskMetricService;
    private final PortfolioService portfolioService;
    private final HistoricalDataService historicalDataService;
    private final AssetService assetService;


    /**
     * Find investment returns for a given portfolio.
     *
     * @param portfolio the portfolio to fetch returns for
     * @return a list of investment returns
     */
    public List<Double> findInvestmentReturns(Portfolio portfolio) {
        return assetService.findInvestmentReturnsByPortfolio(portfolio);
    }


    /**
     * Find market returns (S&P 500) for a given portfolio.
     *
     * @param portfolio the portfolio to fetch the marketreturns for
     * @return a list of market returns relative to the purchase date of each asset in a portfolio
     */
    public List<Double> findMarketReturns(Portfolio portfolio) {

        List<Double> marketReturns = new ArrayList<>();

        for(int i = 0; i < portfolio.getAssets().size(); i++) {
            marketReturns.add(historicalDataService.findMarketReturns(portfolio.getAssets().get(i)));
        }

        return marketReturns;
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

        double totalPortfolioValue = calculateTotalPortfolioValue(portfolio.getAssets());

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

    private double calculateTotalPortfolioValue(List<Asset> assets) {
        double totalValue = 0.0;
        for (Asset asset : assets) {
            totalValue += asset.getQuantity() * asset.getCurrentPrice();
        }
        return totalValue;
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

        List<Double> prices = historicalDataService.fetchHistoricalData(asset);

        // Price at purchase date is being stored first in the list, the current price after

        double marketPriceAtPurchaseDate = prices.get(1);
        double currentMarketPrice = prices.get(2);

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


}
