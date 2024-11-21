package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.HistoricalData;

import java.util.List;

public interface HistoricalDataService {

    List<HistoricalData> findBySymbol(String symbol);

    void save(HistoricalData historicalData);

    void deleteById(Long id);
}
