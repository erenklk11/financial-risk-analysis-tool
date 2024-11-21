package com.erenkalkan.financial_risk_analysis.repository;

import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByUser(User user);
}
