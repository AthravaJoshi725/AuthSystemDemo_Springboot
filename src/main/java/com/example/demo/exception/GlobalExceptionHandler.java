package com.example.demo.exception;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> buildResponse(String message, int status){
        ErrorResponse error = new ErrorResponse(message, status, LocalDateTime.now());

        return ResponseEntity.status(status).body(error);
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex){

        return buildResponse(ex.getMessage(), 404);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex){

        return buildResponse(ex.getMessage(), 409);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgument(MethodArgumentNotValidException ex){

        return buildResponse(ex.getMessage(), 400);
    }

    @ExceptionHandler(IncorrectCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleIncorrectCredentials( IncorrectCredentialsException ex){
        return buildResponse(ex.getMessage(), 409);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> invalidRefreshToken(InvalidRefreshTokenException ex){
        return buildResponse(ex.getMessage(), 409);
    }

}


