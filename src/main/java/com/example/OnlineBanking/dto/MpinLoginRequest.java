package com.example.OnlineBanking.dto;

import lombok.Data;

@Data
public class MpinLoginRequest {
    private Long userId; // The ID identified by the valid JWT Access Token
    private String mpin; // The 6 digits entered on the keypad
}