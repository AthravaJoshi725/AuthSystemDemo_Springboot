package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.*;
import java.util.*;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findBySessionId(String sessionId);
    Optional<UserSession> findBySessionIdAndActiveTrue(String sessionId);
    // void deleteByUser(User user);

    List<UserSession> findByUserAndActiveTrue(User user);

}
