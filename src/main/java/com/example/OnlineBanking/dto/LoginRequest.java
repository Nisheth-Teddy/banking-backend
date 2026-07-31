package com.example.OnlineBanking.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String identifier; // This will catch whatever they type: Email or Mobile
    private String password;
}