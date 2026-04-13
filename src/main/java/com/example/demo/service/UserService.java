package com.example.demo.service;


import java.util.*;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import com.example.demo.model.*;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSessionRepository;
import com.example.demo.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

import com.example.demo.dto.*;
import com.example.demo.exception.*;


@Service
public class UserService{
    
    final UserRepository userRepository;
    final UserSessionRepository userSessionRepository;
    final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, JwtUtil jwtUtil, UserSessionRepository userSessionRepository, RefreshTokenRepository refreshTokenRepository){
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userSessionRepository = userSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    private UserResponse mapToResponse(User user){
        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        
        return response;
    }
    
    // User Register 
    public UserResponse createUser(UserRequest request){
        
        
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        
        if(existingUser.isPresent()){
            throw new DuplicateEmailException("User with this email already exists");
        }
        
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(hashedPassword);
        user.setRole("USER");
        
        User savedUser = userRepository.save(user);
        
        return mapToResponse(savedUser);

    }

    // Login User
    @Transactional
    public AuthResponse loginUser(LoginRequest request){

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
            () -> new UserNotFoundException("Invalid Credentials")
        );

        if(!passwordEncoder.matches(request.getPassword(),user.getPassword())){
            throw new IncorrectCredentialsException("Invalid Credentials");
        }
        
        // deactive old sessions and delete old refresh tokens
        List<UserSession> oldSessions = userSessionRepository.findByUserAndActiveTrue(user);

        for (UserSession session : oldSessions) {
            session.setActive(false);
            refreshTokenRepository.deleteByUserSession(session);
        }
        userSessionRepository.saveAll(oldSessions);

        // Create new sessionId
        UserSession userSession = new UserSession();
        userSession.setSessionId(UUID.randomUUID().toString());
        userSession.setUser(user);
        userSession.setActive(true);
        // Save the session
        userSessionRepository.save(userSession);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), userSession.getSessionId());

        // Generate refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUserSession(userSession);
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);
        refreshToken.setExpiryDate(expiry);

        // save refesh token in db
        refreshTokenRepository.save(refreshToken);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(token);
        response.setRefreshToken(refreshToken.getToken());

        return response;
    }

    // Logout user
    public String logoutUser(String sessionId){
        Optional<UserSession> userSessionOpt = userSessionRepository.findBySessionId(sessionId);

        if(userSessionOpt.isPresent()){
            UserSession userSession = userSessionOpt.get();

            if(userSession.isActive()){
                userSession.setActive(false);
                userSessionRepository.save(userSession);
            }
        }

        return "logout successfull";

    }
    
    public UserResponse getUserById(Long id){

        Optional<User> optionalUser = userRepository.findById(id);
        User user = optionalUser.orElseThrow(
            () -> new UserNotFoundException("User does not exist.")
        );
        // above lambda function is used to throw exception if user is not found, otherwise it returns the user object
        return mapToResponse(user);
    }

    public List<UserResponse> getAllUsers(){

        List<User> users = userRepository.findAll();
        
        List<UserResponse> response = new ArrayList<>();
        for(User user: users){
            response.add(mapToResponse(user));
        }

        return response;
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request){
        Optional<User> op_user = userRepository.findById(id);

        User user = op_user.orElseThrow(() -> new UserNotFoundException("User does not exist."));

        if(request.getName() !=null){
            user.setName(request.getName());
        }
        if((request.getEmail() !=null) && (!(request.getEmail()).equals(user.getEmail())) ){
                Optional<User> existing = userRepository.findByEmail(request.getEmail());

                if(existing.isPresent()){
                    throw new DuplicateEmailException("Email already exists.");
                }
                user.setEmail(request.getEmail());
        }
        if(request.getPassword() !=null){
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);

        return mapToResponse(user);
    }

    public void deleteUser(Long id){

        User user = userRepository.findById(id).orElseThrow(
            () -> new UserNotFoundException("User does not exist.")
        );

        userRepository.delete(user);
    }


}