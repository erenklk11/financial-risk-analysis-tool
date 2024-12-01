package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements  AssetService {

    @Value("${alphavantage.api.url}")
    private static String API_URL;
    @Value("${alphavantage.api.key}")
    private static String API_KEY;


    private final AssetRepository assetRepository;

    @Override
    public List<Asset> findAll(Portfolio portfolio) {
        return assetRepository.findAllByPortfolio(portfolio);
    }


    @Override
    public void save(Asset asset) {
        assetRepository.save(asset);
    }

    @Override
    public void deleteById(Long id) {
        assetRepository.deleteById(id);
    }

    @Override
    public List<Double> fetchPrices(Asset asset, int lastXDays) {

        List<Double> prices = new ArrayList<>();

        try {
            String purchaseDate = asset.getPurchaseDate().toString();
            String currentDate = LocalDate.now().toString();

            URI uri = new URI(API_URL + "?function=TIME_SERIES_DAILY&symbol=" + asset.getSymbol() + "&outputsize=full&apikey=" + API_KEY);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject jsonObject = new JSONObject(response.body());
            JSONObject timeSeries = jsonObject.getJSONObject("Time Series (Daily)");

            if(lastXDays == 1) {
                // Means we only want 1 historical price = The price at the time of purchase
                List<String> datesToCheck = Arrays.asList(purchaseDate, currentDate); // List of dates to check

                for (String date : datesToCheck) {
                    boolean dataFound = false;
                    String currentDateToCheck = date;

                    // Try to get data for the date or go back one day if data is missing
                    while (!dataFound && currentDateToCheck != null) {
                        if (timeSeries.has(currentDateToCheck)) {
                            JSONObject dayData = timeSeries.getJSONObject(currentDateToCheck);
                            prices.add(dayData.getDouble("4. close"));
                            dataFound = true; // Data is found for this date, stop the loop
                        } else {
                            // If no data found for this date, go back 1 day
                            LocalDate dateObj = LocalDate.parse(currentDateToCheck);
                            currentDateToCheck = dateObj.minusDays(1).toString();
                        }
                    }
                }
            }


            else {
                // Fetch the last X days of prices, adjusting for weekends/holidays
                int daysFetched = 0;
                LocalDate targetDate = LocalDate.now();  // Start from today

                while (daysFetched < lastXDays) {
                    String dateStr = targetDate.toString();
                    if (timeSeries.has(dateStr)) {
                        JSONObject dayData = timeSeries.getJSONObject(dateStr);
                        prices.add(dayData.getDouble("4. close"));  // Adjust this to match your desired price (e.g., close price)
                        daysFetched++;
                    }
                    targetDate = targetDate.minusDays(1);  // Move one day back if no data found for this day
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return prices;
    }

    public List<Double> fetchPrices(Asset asset) {
        return fetchPrices(asset, 1);
    }
}
