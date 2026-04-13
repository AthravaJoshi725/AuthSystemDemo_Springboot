package com.example.demo.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UserResponse;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.UserRepository;
import com.example.demo.model.*;

@Service
public class CustomerUserDetailsService implements UserDetailsService{
    
    private UserRepository userRepository;

    CustomerUserDetailsService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        User user = userRepository.findByEmail(username).orElseThrow(
            () -> new UsernameNotFoundException("User does not exist")
        );

        return new CustomerUserDetails(user);

    }
}
