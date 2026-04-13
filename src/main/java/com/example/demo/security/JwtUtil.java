package com.example.demo.security;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "jfsdlfkdjfdsdfeterotuvcmvbasdfghjhgfdsasdfghjhgfdsasdfghjhgfds";
    private static final long EXPIRY = 1000 * 60 * 60; // 1 hour

    // ✅ Generate SecretKey (IMPORTANT: use SecretKey, not Key)
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // ✅ Generate Token
    public String generateToken(String email, String sessionId) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("sessionId", sessionId);
        
        return Jwts.builder().claims(claims)
                .subject(email) 
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRY))
                .signWith(getSigningKey()) // no algorithm needed
                .compact();
    }

    // Extract Email (subject)
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    // Extract sessiondId
    public String extractSessionId(String token){
        return getClaims(token).get("sessionId", String.class);
    }

    // Validate Token
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ Core method: parse + validate token
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())   // NEW in 0.12
                .build()
                .parseSignedClaims(token)     // replaces parseClaimsJws()
                .getPayload();                // replaces getBody()
    }
}