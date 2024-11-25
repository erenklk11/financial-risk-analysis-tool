package com.erenkalkan.financial_risk_analysis.repository;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findAllByPortfolio(Portfolio portfolio);

    @Query("SELECT (a.currentPrice - a.purchasePrice) / a.purchasePrice AS profit FROM Asset a WHERE a.portfolio = :portfolio")
    List<Double> findInvestmentReturnsByPortfolio(@Param("portfolio") Portfolio portfolio);

}
