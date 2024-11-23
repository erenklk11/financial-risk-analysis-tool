package com.erenkalkan.financial_risk_analysis.controller;

import com.erenkalkan.financial_risk_analysis.service.AssetService;
import com.erenkalkan.financial_risk_analysis.service.HistoricalDataService;
import com.erenkalkan.financial_risk_analysis.service.PortfolioService;
import com.erenkalkan.financial_risk_analysis.service.RiskMetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final AssetService assetService;
    private final HistoricalDataService historicalDataService;
    private final PortfolioService portfolioService;
    private final RiskMetricService riskMetricService;


}
