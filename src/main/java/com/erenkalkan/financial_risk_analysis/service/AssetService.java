package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

public interface AssetService {


    Asset findById(Long id);

    List<Asset> findAll(Portfolio portfolio);

    void save(Asset asset);

    void deleteById(Long id);

    List<Double> fetchPrices(Asset asset, int lastXdays);

    List<Double> fetchPrices(Asset asset);
}
