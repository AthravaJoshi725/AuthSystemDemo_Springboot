package com.example.demo.dto;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UpdateUserRequest {
    private String name;
    @Email(message = "Email is not valid")
    private String email;
    private String password;
}
