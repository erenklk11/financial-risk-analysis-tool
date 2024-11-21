package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.RiskMetric;

public interface RiskMetricService {

    RiskMetric findByPortfolio(Portfolio portfolio);

    void save(RiskMetric riskMetric);

    void deleteById(Long id);
}
