package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements  AssetService {


    private final AssetRepository assetRepository;

    @Override
    public List<Asset> findAll(Long portfolioId) {
        return assetRepository.findAllByPortfolio(portfolioId);
    }

    @Override
    public void save(Asset asset) {

    }

    @Override
    public void deleteById(Long id) {

    }
}
