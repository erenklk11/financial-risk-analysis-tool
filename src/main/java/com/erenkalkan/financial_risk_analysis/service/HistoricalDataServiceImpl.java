package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.HistoricalData;
import com.erenkalkan.financial_risk_analysis.repository.HistoricalDataRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoricalDataServiceImpl implements HistoricalDataService {

    @Value("${alphavantage.api.url}")
    private static String API_URL;
    @Value("${alphavantage.api.key}")
    private static String API_KEY;

    private final HistoricalDataRepository historicalDataRepository;


    @Override
    public HistoricalData findByAsset(Asset asset) {
        return historicalDataRepository.findAllByAsset(asset);
    }

    @Override
    public void save(HistoricalData historicalData) {
        historicalDataRepository.save(historicalData);
    }

    @Override
    public void deleteById(Long id) {
        historicalDataRepository.deleteById(id);
    }

    @Override
    public List<Double> fetchHistoricalData(Asset asset) {

        List<Double> purchaseAndCurrentPrice = new ArrayList<>();

        try {
            String purchaseDate = asset.getPurchaseDate().toString();
            String currentDate = LocalDate.now().toString();

            URI uri = new URI(API_URL + "?function=TIME_SERIES_DAILY&symbol=SPY&outputsize=full&apikey=" + API_KEY);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject jsonObject = new JSONObject(response.body());
            JSONObject timeSeries = jsonObject.getJSONObject("Time Series (Daily)");

            if (timeSeries.has(purchaseDate)) {
                JSONObject dayData = timeSeries.getJSONObject(purchaseDate);
                purchaseAndCurrentPrice.add(dayData.getDouble(purchaseDate));
            }
            if (timeSeries.has(currentDate)) {
                JSONObject dayData = timeSeries.getJSONObject(currentDate);
                purchaseAndCurrentPrice.add(dayData.getDouble(currentDate));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return purchaseAndCurrentPrice;
    }

    @Override
    public double findMarketReturns(Asset asset) {
        return historicalDataRepository.findMarketReturns(asset);
    }


}
