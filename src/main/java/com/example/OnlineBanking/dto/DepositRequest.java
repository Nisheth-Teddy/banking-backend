package com.example.OnlineBanking.dto;

import lombok.Data;

import java.security.PrivateKey;
import java.security.SecureRandom;

@Data
public class DepositRequest {
    private String accountNumber;
    private String ifscCode;
    private Double amount;
}