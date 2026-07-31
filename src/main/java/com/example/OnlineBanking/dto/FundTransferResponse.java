package com.example.OnlineBanking.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class FundTransferResponse {
    private String transactionId;
    private String message;
    private Double remainingBalance;
}