package com.example.OnlineBanking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;

@Data
public class OpenAccountRequest {
    private Long userId; // Passed from the frontend session to link the profile
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNo;
    private String identityCardNumber;
    private String parentName;
    private String parentPhoneNo;
    private String address;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    
    private String mpin;// The 6-digit MPIN they pick

    private String confirmMpin;
}