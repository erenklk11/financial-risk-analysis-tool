package com.erenkalkan.financial_risk_analysis.util;

import com.erenkalkan.financial_risk_analysis.repository.PortfolioRepository;
import com.erenkalkan.financial_risk_analysis.repository.RiskMetricRepository;
import com.erenkalkan.financial_risk_analysis.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskMetricHelper {

    private final RiskMetricRepository riskMetricRepository;
    private final PortfolioRepository portfolioRepository;



}
