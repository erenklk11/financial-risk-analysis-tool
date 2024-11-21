package com.erenkalkan.financial_risk_analysis.controller;

import com.erenkalkan.financial_risk_analysis.entity.User;
import com.erenkalkan.financial_risk_analysis.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@RequiredArgsConstructor
@Controller
public class LoginController {

    private final UserService userService;

    @GetMapping("/")
    public String signIn() {
        return "sign-in";
    }

    @RequestMapping("/welcome")
    public String welcome() {
        return "welcome";
    }

    @GetMapping("/sign-up")
    public String signUp(Model model) {

        model.addAttribute("newUser", new User());

        return "sign-up";
    }

    @PostMapping("/processSignUp")
    public String processSignUp(@ModelAttribute("newUser") User user, Model model) {

        try {
            Optional<User> existingUser = userService.findByUsername(user.getUsername());
            Optional<User> existingEmail = userService.findByEmail(user.getEmail());

            if (existingUser.isPresent()) {
                model.addAttribute("error", "Username is already taken. Please choose another.");
                return "sign-up";
            }

            if (existingEmail.isPresent()) {
                model.addAttribute("error", "Email is already in use. Please use another.");
                return "sign-up";
            }

            userService.save(user);

            return "redirect:/welcome";

        } catch (Exception e) {
            model.addAttribute("error", "An error occurred during sign-up. Please try again.");
            return "sign-up";
        }
    }


}
