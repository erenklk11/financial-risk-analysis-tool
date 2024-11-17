package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

public interface AssetService {


    List<Asset> findAll(Long portfolioId);

    void save(Asset asset);

    void deleteById(Long id);

}
