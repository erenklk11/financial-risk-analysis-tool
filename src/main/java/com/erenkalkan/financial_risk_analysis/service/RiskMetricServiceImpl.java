package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.RiskMetric;
import com.erenkalkan.financial_risk_analysis.repository.RiskMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskMetricServiceImpl implements RiskMetricService{

    private final RiskMetricRepository riskMetricRepository;


    @Override
    public RiskMetric findByPortfolio(Portfolio portfolio) {
        return riskMetricRepository.findByPortfolio(portfolio);
    }

    @Override
    public void save(RiskMetric riskMetric) {
        riskMetricRepository.save(riskMetric);
    }

    @Override
    public void deleteById(Long id) {
        riskMetricRepository.deleteById(id);
    }
}
