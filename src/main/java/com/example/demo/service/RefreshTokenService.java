package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.exception.InvalidRefreshTokenException;
import com.example.demo.model.RefreshToken;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.security.JwtUtil;


@Service
public class RefreshTokenService {
    
    private RefreshTokenRepository refreshTokenRepository;
    private JwtUtil jwtUtil;
    public RefreshTokenService(RefreshTokenRepository
         refreshTokenRepository, JwtUtil jwtUtil
    ){
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
    }


    public AuthResponse generateAccessToken(RefreshTokenRequest request){
        
        // check if exists
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken()).orElseThrow(
            () -> new InvalidRefreshTokenException("RefreshToken Invalid")
        );


        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        if (!refreshToken.getUserSession().isActive()) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
   
        String email = refreshToken.getUserSession().getUser().getEmail();
        String sessionId = refreshToken.getUserSession().getSessionId();

        String newAccessToken =  jwtUtil.generateToken(email, sessionId);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(newAccessToken);

        return response;
    }
}
