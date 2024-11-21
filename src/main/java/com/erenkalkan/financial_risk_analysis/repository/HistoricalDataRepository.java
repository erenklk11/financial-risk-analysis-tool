package com.erenkalkan.financial_risk_analysis.repository;

import com.erenkalkan.financial_risk_analysis.entity.HistoricalData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistoricalDataRepository extends JpaRepository<HistoricalData, Long> {

    List<HistoricalData> findAllBySymbol(String symbol);
}
