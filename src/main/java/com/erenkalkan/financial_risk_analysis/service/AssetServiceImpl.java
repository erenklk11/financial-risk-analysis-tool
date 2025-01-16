package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.controller.MainController;
import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements  AssetService {

    @Value("${alphavantage.api.url}")
    private String API_URL;
    @Value("${alphavantage.api.key}")
    private String API_KEY;


    private final AssetRepository assetRepository;


    @Override
    public Asset findById(Long id) {
        return assetRepository.findById(id).orElseThrow();
    }

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

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Override
    public List<Double> fetchPrices(Asset asset, int lastXDays) {
        List<Double> prices = new ArrayList<>();

        try {
            if (asset == null) {
                throw new IllegalArgumentException("Asset cannot be null");
            }
            if (asset.getSymbol() == null) {
                throw new IllegalArgumentException("Asset symbol cannot be null");
            }
            if (API_URL == null) {
                throw new IllegalStateException("API URL is null. Check your application.properties configuration.");
            }


            log.debug("Fetching prices for symbol: {} with lastXDays: {}", asset.getSymbol(), lastXDays);
            log.debug("Using API URL: {}", API_URL);

            // Validate and clean the API URL
            String cleanApiUrl = API_URL.trim();
            if (!cleanApiUrl.startsWith("http://") && !cleanApiUrl.startsWith("https://")) {
                cleanApiUrl = "https://" + cleanApiUrl;
            }

            // Build the complete URL string first for logging
            String urlString = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=full&apikey=%s",
                    cleanApiUrl,
                    URLEncoder.encode(asset.getSymbol(), StandardCharsets.UTF_8),
                    API_KEY);

            log.debug("Constructed URL: {}", urlString);

            // Create URI from the validated URL
            URI uri = new URI(urlString);

            log.debug("Making API request to URI: {}", uri);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("Received response with status code: {}", response.statusCode());

            String responseBody = response.body();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new RuntimeException("Empty response received from API");
            }

//            log.debug("Response body: {}", responseBody);

            // Check if response is successful
            if (response.statusCode() != 200) {
                throw new RuntimeException("API request failed with status code: " + response.statusCode());
            }

            // Validate that we have a response body
            if (response.body() == null || response.body().trim().isEmpty()) {
                throw new RuntimeException("Empty response received from API");
            }

            // Parse JSON response
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(response.body());
            } catch (JSONException e) {
                log.error("Failed to parse JSON response: {}", response.body(), e);
                throw new RuntimeException("Invalid JSON response from API", e);
            }

            // Check for API error messages
            if (jsonObject.has("Error Message")) {
                String errorMessage = jsonObject.getString("Error Message");
                log.error("API returned error: {}", errorMessage);
                throw new RuntimeException("API Error: " + errorMessage);
            }

            // Check for rate limit message
            if (jsonObject.has("Note")) {
                String note = jsonObject.getString("Note");
                log.warn("API returned note: {}", note);
                if (note.contains("API call frequency")) {
                    throw new RuntimeException("API rate limit reached. Please try again in a minute.");
                }
            }

            // Validate time series data exists
            if (!jsonObject.has("Time Series (Daily)")) {
                log.error("Response missing Time Series data: {}", jsonObject.toString());
                throw new RuntimeException("No time series data available for symbol: " + asset.getSymbol());
            }

            JSONObject timeSeries = jsonObject.getJSONObject("Time Series (Daily)");
            if (timeSeries.length() == 0) {
                throw new RuntimeException("Empty time series data received");
            }

            if (lastXDays <= 1) {
                String purchaseDate = asset.getPurchaseDate().toString();
                String currentDate = LocalDate.now().toString();
                // Handle both current and historical single-day price fetching
                List<String> datesToCheck = Arrays.asList(lastXDays == 0 ? currentDate : purchaseDate);

                for (String date : datesToCheck) {
                    boolean dataFound = false;
                    LocalDate checkDate = LocalDate.parse(date);

                    // Try up to 5 business days back to find data
                    for (int i = 0; i < 5 && !dataFound; i++) {
                        String dateToCheck = checkDate.toString();
                        if (timeSeries.has(dateToCheck)) {
                            JSONObject dayData = timeSeries.getJSONObject(dateToCheck);
                            if (dayData.has("4. close")) {
                                prices.add(dayData.getDouble("4. close"));
                                dataFound = true;
                                log.debug("Found price for date {}: {}", dateToCheck, dayData.getDouble("4. close"));
                            }
                        }
                        checkDate = checkDate.minusDays(1);
                    }

                    if (!dataFound) {
                        log.error("No price data found within 5 days of {}", date);
                        throw new RuntimeException("No price data found within 5 days of " + date);
                    }
                }
            } else {
                // Fetch multiple days of prices
                LocalDate targetDate = LocalDate.now();
                int daysFetched = 0;
                int maxAttempts = lastXDays * 2; // Allow for weekends/holidays
                int attempts = 0;

                while (daysFetched < lastXDays && attempts < maxAttempts) {
                    String dateStr = targetDate.toString();
                    if (timeSeries.has(dateStr)) {
                        JSONObject dayData = timeSeries.getJSONObject(dateStr);
                        prices.add(dayData.getDouble("4. close"));
                        daysFetched++;
                    }
                    targetDate = targetDate.minusDays(1);
                    attempts++;
                }

                if (daysFetched < lastXDays) {
                    throw new RuntimeException("Could not fetch enough price data. Requested: " + lastXDays + ", Found: " + daysFetched);
                }
            }

        } catch (URISyntaxException e) {
            log.error("Invalid URI syntax", e);
            throw new RuntimeException("Invalid URI syntax: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Network error while fetching prices", e);
            throw new RuntimeException("Network error while fetching prices: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error("Operation interrupted while fetching prices", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operation interrupted while fetching prices", e);
        } catch (JSONException e) {
            log.error("Error parsing JSON response", e);
            throw new RuntimeException("Error parsing JSON response: " + e.getMessage(), e);
        }

        return prices;
    }

    // Fetching price of today by default
    public List<Double> fetchPrices(Asset asset) {
        return fetchPrices(asset, 0);
    }
}
