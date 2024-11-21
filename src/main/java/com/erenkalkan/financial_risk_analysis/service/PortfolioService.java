package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.User;

import java.util.Optional;

public interface PortfolioService {

    Portfolio findByUser(User user);

    void save(Portfolio portfolio);

    void deleteById(Long id);
}
