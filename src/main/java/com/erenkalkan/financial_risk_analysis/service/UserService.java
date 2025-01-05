package com.erenkalkan.financial_risk_analysis.service;

import com.erenkalkan.financial_risk_analysis.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User findByUsername(String username);

    User findByEmail(String email);

    List<User> findAll();

    void save(User user);

    void deleteById(Long id);
}
