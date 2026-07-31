package com.example.OnlineBanking.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(nullable = false)
    private String accountNumber;// The account this record belongs to

    @Column(nullable = true)
    private String phoneNo;

    @Column(nullable = false)
    private String transactionType; // "DEPOSIT", "WITHDRAWAL", "TRANSFER_SENT", "TRANSFER_RECEIVED"

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private Double postTransactionBalance; // The balance *after* this specific action took place

    private String description; // e.g., "Cash Deposit via Admin Counter" or "Transferred to SB1023"

    @Column(nullable = false)
    private LocalDateTime timestamp;
}