package com.erenkalkan.financial_risk_analysis.controller;

import com.erenkalkan.financial_risk_analysis.entity.Asset;
import com.erenkalkan.financial_risk_analysis.entity.Portfolio;
import com.erenkalkan.financial_risk_analysis.entity.RiskMetric;
import com.erenkalkan.financial_risk_analysis.entity.User;
import com.erenkalkan.financial_risk_analysis.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Controller
public class LoginController {

    private final UserService userService;
    private final AssetService assetService;
    private final HistoricalDataService historicalDataService;
    private final PortfolioService portfolioService;
    private final RiskMetricService riskMetricService;
    private final AuthenticationManager authenticationManager;


    @GetMapping("/")
    public String signIn() {
        return "sign-in";
    }

    @RequestMapping("/welcome")
    public String welcome(Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userService.findByUsername(username);

        model.addAttribute("username", username);

        return "welcome";
    }

    @GetMapping("/sign-up")
    public String signUp(Model model) {

        model.addAttribute("newUser", new User());

        return "sign-up";
    }

    @PostMapping("/processSignUp")
    public String processSignUp(@ModelAttribute("newUser") User user, Model model, HttpServletRequest request) {

        try {
            User existingUser = userService.findByUsername(user.getUsername());
            User existingEmail = userService.findByEmail(user.getEmail());

            if (existingUser == null) {
                model.addAttribute("error", "Username is already taken. Please choose another.");
                return "sign-up";
            }

            if (existingEmail == null) {
                model.addAttribute("error", "Email is already in use. Please use another.");
                return "sign-up";
            }

            String rawPassword = user.getPassword();

            userService.save(user);

            authWithAuthManager(request, user.getUsername(), rawPassword);

            return "redirect:/welcome";

        } catch (Exception e) {
            model.addAttribute("error", "An error occurred during sign-up. Please try again.");
            return "sign-up";
        }
    }

    public void authWithAuthManager(HttpServletRequest request, String username, String password) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
        authToken.setDetails(new WebAuthenticationDetails(request));

        Authentication authentication = authenticationManager.authenticate(authToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
