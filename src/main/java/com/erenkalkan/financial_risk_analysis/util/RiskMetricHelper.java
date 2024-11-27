package com.erenkalkan.financial_risk_analysis.util;

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




}
