package com.example.OnlineBanking.controller;

import com.example.OnlineBanking.model.Transaction;
import com.example.OnlineBanking.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bank/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // FETCH PASSBOOK/STATEMENT
    @GetMapping("/statement/{accountNumber}")
    public ResponseEntity<?> getAccountStatement(@PathVariable String accountNumber) {
        try {
            List<Transaction> statement = transactionRepository.findByAccountNumberOrderByTimestampDesc(accountNumber);

            if (statement.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "No transactions found for this account.",
                        "statement", List.of()
                ));
            }

            return ResponseEntity.ok(statement);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}