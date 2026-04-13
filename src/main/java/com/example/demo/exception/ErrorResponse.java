package com.example.demo.exception;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class ErrorResponse {
    private String message;
    private int status;
    private LocalDateTime timestamp;

    public ErrorResponse(String message, int status, LocalDateTime timestamp){
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }
}
