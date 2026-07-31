package com.example.OnlineBanking.dto;

import lombok.Data;

@Data
public class MpinResetRequest {
    private String accountNumber;
    private String ifscCode;
    private String newMpin;
}