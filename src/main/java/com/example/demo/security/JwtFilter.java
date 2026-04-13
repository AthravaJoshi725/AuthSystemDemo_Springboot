package com.example.demo.security;

import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.model.UserSession;
import com.example.demo.repository.UserSessionRepository;
import com.example.demo.service.CustomerUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomerUserDetailsService userDetailsService;
    private UserSessionRepository userSessionRepository;
    public JwtFilter(JwtUtil jwtUtil, CustomerUserDetailsService userDetailsService, UserSessionRepository userSessionRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userSessionRepository = userSessionRepository;

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain filterChain) throws ServletException, IOException{

    System.out.println("Filter running...");
    
    String authHeader = request.getHeader("Authorization");

    if(authHeader != null && authHeader.startsWith("Bearer ")){
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        System.out.println(token);

        if(email!=null && SecurityContextHolder.getContext().getAuthentication() == null){

            if(jwtUtil.isTokenValid(token)){

                // extract sessionId
                String sessionId = jwtUtil.extractSessionId(token);
                // Check db
                Optional<UserSession> userSession = userSessionRepository.findBySessionIdAndActiveTrue(sessionId);
                request.setAttribute("sessionId", sessionId);
                if(userSession.isPresent()){

                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,null, userDetails.getAuthorities());
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        }        
    }


    filterChain.doFilter(request, response);

    }
}
