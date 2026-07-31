package com.example.OnlineBanking.controller;

import com.example.OnlineBanking.dto.DepositRequest;
import com.example.OnlineBanking.model.AccountHolder;
import com.example.OnlineBanking.model.User;
import com.example.OnlineBanking.repository.UserRepository;
import com.example.OnlineBanking.service.AccountHolderService;
import com.example.OnlineBanking.repository.AccountHolderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bank/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AccountHolderService accountHolderService;
    private final AccountHolderRepository accountHolderRepository;


    public AdminController(AccountHolderService accountHolderService, AccountHolderRepository accountHolderRepository) {
        this.accountHolderService = accountHolderService;
        this.accountHolderRepository = accountHolderRepository;
    }

    // 1. CASH DEPOSIT COUNTER (Tellers use this for physical cash handling)
    @PostMapping("/deposit")
    public ResponseEntity<?> cashCounterDeposit(@RequestBody DepositRequest request) {
        try {

            AccountHolder updatedAccount = accountHolderService.depositFunds(request);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Cash Counter Deposit successful.",
                    "accountNumber", updatedAccount.getAccountNumber(),
                    "ifscCode",updatedAccount.getIfscCode(),
                    "updatedBalance", updatedAccount.getBalance()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 2. SEARCH/VIEW ALL CUSTOMER DETAILS (For Audit and Bank Officer verification)
    @GetMapping("/customers")
    public ResponseEntity<List<AccountHolder>> getAllBankCustomers() {
        List<AccountHolder> customers = accountHolderRepository.findAll();
        return ResponseEntity.ok(customers);
    }
}