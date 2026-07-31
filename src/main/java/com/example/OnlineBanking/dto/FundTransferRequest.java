package com.example.OnlineBanking.dto;

import lombok.Data;

@Data
public class FundTransferRequest {
    private String targetPhoneNo;
    private Double amount;
    private String mpin; // The 6-digit MPIN required to authorize the movement
    private String remarks;
}