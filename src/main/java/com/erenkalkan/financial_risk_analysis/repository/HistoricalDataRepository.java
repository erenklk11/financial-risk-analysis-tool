package com.erenkalkan.financial_risk_analysis.repository;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.HistoricalData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoricalDataRepository extends JpaRepository<HistoricalData, Long> {

    HistoricalData findAllByAsset(Asset asset);

    @Query("SELECT (a.currentPrice - a.purchasePrice) / a.purchasePrice AS profit FROM HistoricalData a WHERE a.asset = :asset")
    double findMarketReturns(@Param("asset") Asset asset);

}
