package com.example.OnlineBanking.dto;

import lombok.Data;

@Data
public class AdminSignupRequest {
    private String identifier; // Admin Email
    private String password;
    private String confirmPassword;
    private String secretAdminKey; // The golden ticket key
}