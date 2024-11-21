package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.HistoricalData;
import com.erenkalkan.financial_risk_analysis.repository.HistoricalDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoricalDataServiceImpl implements HistoricalDataService{

    private final HistoricalDataRepository historicalDataRepository;


    @Override
    public List<HistoricalData> findBySymbol(String symbol) {
        return historicalDataRepository.findAllBySymbol(symbol);
    }

    @Override
    public void save(HistoricalData historicalData) {
        historicalDataRepository.save(historicalData);
    }

    @Override
    public void deleteById(Long id) {
        historicalDataRepository.deleteById(id);
    }
}
