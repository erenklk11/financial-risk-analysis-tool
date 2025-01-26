package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.User;
import com.erenkalkan.financial_risk_analysis.repository.PortfolioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService{

    private final PortfolioRepository portfolioRepository;

    @Override
    public Portfolio findByUser(User user) {
        return portfolioRepository.findByUser(user).orElseThrow(() -> new RuntimeException("Portfolio not found with user: " + user));
    }

    @Override
    @Transactional
    public void save(Portfolio portfolio) {
        portfolioRepository.save(portfolio);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        portfolioRepository.deleteById(id);
    }
}
