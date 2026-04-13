package com.example.demo.exception;

import ch.qos.logback.classic.pattern.MethodOfCallerConverter;

public class MethodArgumentNotValidException extends Exception{
    MethodArgumentNotValidException(String message){
        super(message);
    }
}
