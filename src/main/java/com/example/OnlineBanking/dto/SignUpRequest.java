package com.example.OnlineBanking.dto;

import lombok.Data;

@Data
public class SignUpRequest {
    private String identifier; // Mobile or Email
    private String password;
    private String confirmPassword;
}