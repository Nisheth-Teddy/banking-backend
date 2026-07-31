package com.example.OnlineBanking.controller;

import com.example.OnlineBanking.dto.FundTransferRequest;
import com.example.OnlineBanking.dto.FundTransferResponse;
import com.example.OnlineBanking.dto.MpinResetRequest;
import com.example.OnlineBanking.dto.OpenAccountRequest;
import com.example.OnlineBanking.model.AccountHolder;
import com.example.OnlineBanking.service.AccountHolderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/bank/user")
@CrossOrigin(origins = "*")
public class AccountHolderController {

    private final AccountHolderService accountHolderService;

    public AccountHolderController(AccountHolderService accountHolderService) {
        this.accountHolderService = accountHolderService;
    }

    @PostMapping("/open-account")
    public ResponseEntity<?> openAccount(@RequestBody OpenAccountRequest request) {
        try {
            AccountHolder newAccount = accountHolderService.openBankingAccount(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status","Success",
                    "message", "Bank account created successfully!",
                    "accountHolderId",newAccount.getId(),
                    "fullName",newAccount.getFirstName().concat(" ").concat(newAccount.getLastName()),
                    "accountNumber", newAccount.getAccountNumber(),
                    "ifscCode", newAccount.getIfscCode(),
                    "initialBalance", newAccount.getBalance()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/banking/dashboard -> Loads balance and user info into Layout Model 1 automatically
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData() {
        try {
            String currentLoggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
            AccountHolder profile = accountHolderService.getProfileByUsername(currentLoggedUser);

            return ResponseEntity.ok(Map.of(
                    "firstName", profile.getFirstName(),
                    "lastName", profile.getLastName(),
                    "accountNumber", profile.getAccountNumber(),
                    "ifscCode", profile.getIfscCode(),
                    "currentBalance", profile.getBalance()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/banking/transfer -> Triggered when pressing "Send" and confirming the MPIN pop-up modal
    @PostMapping("/transfer")
    public ResponseEntity<?> processFundTransfer(@RequestBody FundTransferRequest request) {
        try {
            String currentLoggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
            FundTransferResponse response = accountHolderService.transferFunds(currentLoggedUser, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    // 2. MPIN RESET USING ACCOUNT DETAILS (When they click "Forgot MPIN" on that same page)
    @PostMapping("/reset-mpin")
    public ResponseEntity<?> resetMpinWithAccountDetails(@RequestBody MpinResetRequest request) {
        try {
            // Automatically extract who is logged in from the JWT token sent by the browser
            String currentLoggedUser = SecurityContextHolder.getContext().getAuthentication().getName();

            accountHolderService.resetMpinWithAuthenticatedDetails(currentLoggedUser, request);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Your MPIN has been reset successfully! You can now verify and enter."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}