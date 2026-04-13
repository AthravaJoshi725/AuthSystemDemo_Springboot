package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.UserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    final UserService userService;
    final RefreshTokenService refreshTokenService;

    public AuthController(UserService userService, RefreshTokenService refreshTokenService){
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }
    // User login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid  @RequestBody LoginRequest request){

        AuthResponse response = userService.loginUser(request);

        return ResponseEntity.ok(response);
    }

    // User register
    @PostMapping("/register")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request){
        System.out.println("Received registration request for email: " + request.getEmail());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // User logout
    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpServletRequest request){
        
        String sessionId = (String) request.getAttribute("sessionId");

        String response = userService.logoutUser(sessionId);

        return ResponseEntity.ok(response);

    }

    // User refresh token
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request){
        
        AuthResponse response = refreshTokenService.generateAccessToken(request);

        return ResponseEntity.ok(response);
    }
}
