package com.erenkalkan.financial_risk_analysis.controller;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.RiskMetric;
import com.erenkalkan.financial_risk_analysis.entity.User;
import com.erenkalkan.financial_risk_analysis.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;


import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    @Value("${alphavantage.api.url}")
    private String STOCK_API_URL;
    @Value("${alphavantage.api.key}")
    private String STOCK_APIKEY;

    private final AssetService assetService;
    private final UserService userService;
    private final HistoricalDataService historicalDataService;
    private final PortfolioService portfolioService;
    private final RiskMetricService riskMetricService;

    @RequestMapping("/home")
    public String getHome(Model model) {

        User user = getCurrentUser();

        // Initialize variables
        Portfolio portfolio = null;
        List<Asset> assets = new ArrayList<>();
        RiskMetric riskMetric = null;

        try {
            portfolio = portfolioService.findByUser(user);
            if (portfolio != null) {
                assets = assetService.findAll(portfolio);
                riskMetric = riskMetricService.findByPortfolio(portfolio);
            }
        } catch (RuntimeException e) {
            // Log error
            System.err.println(e + "\nPortfolio not found for user: " + user);
        }

        // Add attributes to model
        model.addAttribute("username", user.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("portfolio", portfolio);
        model.addAttribute("assets", assets);
        model.addAttribute("riskMetric", riskMetric);

        return "home";
    }


    @PostMapping("/home/create")
    public String createPortfolio(@RequestParam String name) {

        User user = getCurrentUser();

        // Create new portfolio
        Portfolio portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setUser(user);
        portfolioService.save(portfolio);

        // Redirect to home page to see the new portfolio
        return "redirect:/home";
    }


    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    @PostMapping("/home/assets/add")
    public String addAsset(@RequestParam("symbol") String symbol,
                           @RequestParam("purchaseDate") String purchaseDate,
                           @RequestParam("quantity") int quantity,
                           Model model) {
        try {
            log.debug("Starting asset addition process for symbol: {}", symbol);

            // Validate symbol
            String url = STOCK_API_URL + "?function=SYMBOL_SEARCH&keywords=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8) + "&apikey=" + STOCK_APIKEY;
            RestTemplate restTemplate = new RestTemplate();

            log.debug("Calling Alpha Vantage API for symbol validation");
            ResponseEntity<Map> responseEntity = restTemplate.getForEntity(url, Map.class);

            if (responseEntity.getBody() == null) {
                log.error("Received null response body from Alpha Vantage API");
                model.addAttribute("errorMessage", "Unable to validate stock symbol. Please try again later.");
                return "home";
            }

            Map<String, Object> response = responseEntity.getBody();
            log.debug("Received response from Alpha Vantage: {}", response);

            boolean isValid = false;
            String stockName = null;
            if (response.containsKey("bestMatches")) {
                List<Map<String, String>> bestMatches = (List<Map<String, String>>) response.get("bestMatches");
                for (Map<String, String> match : bestMatches) {
                    if (symbol.equalsIgnoreCase(match.get("1. symbol"))) {
                        isValid = true;
                        stockName = match.get("2. name");
                        break;
                    }
                }
                log.debug("Symbol validation result: {}", isValid);
            }

            if (!isValid || stockName == null) {
                model.addAttribute("errorMessage", "Invalid stock symbol. Please try again.");
                return "home";
            }

            // Parse purchaseDate into LocalDate
            LocalDate parsedDate;
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                parsedDate = LocalDate.parse(purchaseDate, formatter);
            } catch (DateTimeParseException e) {
                log.error("Date parsing error for {}: {}", purchaseDate, e.getMessage());
                model.addAttribute("errorMessage", "Invalid date format. Use 'yyyy-MM-dd'.");
                return "home";
            }

            User user = getCurrentUser();
            if (user == null) {
                log.error("No user found in current session");
                model.addAttribute("errorMessage", "User session expired. Please login again.");
                return "redirect:/login";
            }

            Portfolio userPortfolio = portfolioService.findByUser(user);
            if (userPortfolio == null) {
                log.error("No portfolio found for user: {}", user.getId());
                model.addAttribute("errorMessage", "Portfolio not found. Please contact support.");
                return "home";
            }

            Asset temp = new Asset();
            temp.setSymbol(symbol);
            temp.setName(stockName);  // Set the name from API response
            temp.setQuantity(Double.valueOf(quantity));
            temp.setPurchaseDate(parsedDate);
            temp.setPortfolio(userPortfolio);

            log.debug("Fetching current price for symbol: {}", symbol);
            List<Double> currentPrices = assetService.fetchPrices(temp, 0);
            if (currentPrices == null || currentPrices.isEmpty()) {
                log.error("Failed to fetch current price for symbol: {}", symbol);
                model.addAttribute("errorMessage", "Unable to fetch current price. Please try again later.");
                return "home";
            }
            temp.setCurrentPrice(currentPrices.getFirst());

            log.debug("Fetching historical price for symbol: {}", symbol);
            List<Double> historicalPrices = assetService.fetchPrices(temp, 1);
            if (historicalPrices == null || historicalPrices.isEmpty()) {
                log.error("Failed to fetch historical price for symbol: {}", symbol);
                model.addAttribute("errorMessage", "Unable to fetch historical price. Please try again later.");
                return "home";
            }
            temp.setPurchasePrice(historicalPrices.getFirst());

            // Set creation timestamp if not already handled by @PrePersist
            if (temp.getCreatedAt() == null) {
                temp.setCreatedAt(LocalDateTime.now());
            }

            log.debug("Saving asset: {}", temp);
            assetService.save(temp);

            log.info("Successfully added new asset: {} ({}) for user: {}", stockName, symbol, user.getId());
            return "redirect:/home";

        } catch (Exception e) {
            log.error("Error while adding asset: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            return "home";
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.findByUsername(username);
    }
}
