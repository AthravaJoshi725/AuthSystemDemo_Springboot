package com.example.demo.exception;

public class IncorrectCredentialsException extends RuntimeException {
    public IncorrectCredentialsException(String message){
        super(message);
    }
}
