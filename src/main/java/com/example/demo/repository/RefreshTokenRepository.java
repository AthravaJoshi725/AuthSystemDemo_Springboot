package com.example.demo.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.*;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserSession(UserSession userSession);
    void deleteByUserSession(UserSession userSession);
}
