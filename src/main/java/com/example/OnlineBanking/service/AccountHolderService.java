package com.example.OnlineBanking.service;

import com.example.OnlineBanking.dto.*;
import com.example.OnlineBanking.model.AccountHolder;
import com.example.OnlineBanking.model.Transaction;
import com.example.OnlineBanking.model.User;
import com.example.OnlineBanking.repository.AccountHolderRepository;
import com.example.OnlineBanking.repository.TransactionRepository;
import com.example.OnlineBanking.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class AccountHolderService {

    private final AccountHolderRepository accountHolderRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AccountHolderService(AccountHolderRepository accountHolderRepository,
                                UserRepository userRepository, TransactionRepository transactionRepository,
                                BCryptPasswordEncoder passwordEncoder) {
        this.accountHolderRepository = accountHolderRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AccountHolder openBankingAccount(OpenAccountRequest request) {
        // 1. Fetch the user credentials base this profile belongs to
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User authorization record not found."));


        // 🌟 MATCH CHECKING LOGIC GATEWAY:
        if (request.getMpin() == null || request.getConfirmMpin() == null) {
           throw new RuntimeException("{\"error\": \"MPIN and Confirm MPIN fields cannot be empty.\"}");
        }

        if (!request.getMpin().equals(request.getConfirmMpin())) {
            throw new RuntimeException("{\"error\": \"Validation Fault: The entered MPIN values do not match.\"}");
        }

        // 2. Validate MPIN format
        if (request.getMpin() == null || !request.getMpin().matches("\\d{6}")) {
            throw new RuntimeException("MPIN must be exactly 6 numeric digits!");
        }


        // 3. Map details into the AccountHolder entity
        AccountHolder holder = new AccountHolder();
        holder.setUser(user);
        holder.setFirstName(request.getFirstName());
        holder.setLastName(request.getLastName());
        holder.setEmail(request.getEmail());
        holder.setPhoneNo(request.getPhoneNo());
        holder.setIdentityCardNumber(request.getIdentityCardNumber());
        holder.setParentName(request.getParentName());
        holder.setParentPhoneNo(request.getParentPhoneNo());
        holder.setAddress(request.getAddress());
        holder.setDateOfBirth(request.getDateOfBirth());

        // 4. Encrypt the 6-digit MPIN for high-security storage
        holder.setMpin(passwordEncoder.encode(request.getMpin()));

        // 5. Simulate Core Banking Auto-Generation features
        holder.setAccountNumber("DB" + (1000000000L + new Random().nextInt(900000000)));
        holder.setIfscCode("VER"+(100000L + new Random().nextInt(900000)));
        holder.setBalance(0.0); // Starts with zero balance until first deposit

        return accountHolderRepository.save(holder);
    }

    // 1. SECURE FETCH PROFILE & BALANCE
    @Transactional(readOnly = true)
    public AccountHolder  getProfileByUsername(String identifier) {
        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("Session identity not found."));
        return accountHolderRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("No active banking profile found."));
    }

    // 2. SECURE FUND TRANSFER MECHANISM
    @Transactional
    public FundTransferResponse transferFunds(String senderIdentifier, FundTransferRequest request) {
        // Find Sender
        User senderUser = userRepository.findByIdentifier(senderIdentifier)
                .orElseThrow(() -> new RuntimeException("Sender identity missing."));
        AccountHolder senderAccount = accountHolderRepository.findByUser(senderUser)
                .orElseThrow(() -> new RuntimeException("Sender banking profile not found."));

        // Rule 1: Validate MPIN
        if (!passwordEncoder.matches(request.getMpin(), senderAccount.getMpin())) {
            throw new RuntimeException("Transaction Denied: Invalid 6-digit MPIN.");
        }

        // Rule 2: Prevent self-transfers
        if (senderAccount.getPhoneNo().equals(request.getTargetPhoneNo())) {
            throw new RuntimeException("Transaction Denied: Cannot transfer funds to your own account.");
        }

        // Rule 3: Check Balance Availability
        if (senderAccount.getBalance() < request.getAmount()) {
            throw new RuntimeException("Transaction Denied: Insufficient funds available.");
        }

        if (request.getAmount() <= 0) {
            throw new RuntimeException("Deposit amount must be greater than zero.");
        }

        // Find Receiver
        AccountHolder receiverAccount = accountHolderRepository.findByPhoneNo(request.getTargetPhoneNo())
                .orElseThrow(() -> new RuntimeException("Transaction Denied: Phone Number Invalid."));

        // Execute Money Movement
        senderAccount.setBalance(senderAccount.getBalance() - request.getAmount());
        receiverAccount.setBalance(receiverAccount.getBalance() + request.getAmount());

        // Save modifications back to MySQL
        accountHolderRepository.save(senderAccount);
        accountHolderRepository.save(receiverAccount);

        // Generate an immutable transaction reference token
        String transactionRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Ledger Entry 1: SENDER DEBIT RECORD
        Transaction debitTx = new Transaction();
        debitTx.setAccountNumber(senderAccount.getAccountNumber());
        debitTx.setPhoneNo(senderAccount.getPhoneNo());
        debitTx.setTransactionType("AMOUNT_SENT");
        debitTx.setAmount(request.getAmount());
        debitTx.setPostTransactionBalance(senderAccount.getBalance());
        debitTx.setDescription("Sent to A/C: " + receiverAccount.getAccountNumber() + " (Ref: " + transactionRef + "). Remarks: " + request.getRemarks());
        debitTx.setTimestamp(LocalDateTime.now());
        transactionRepository.saveAndFlush(debitTx);

        // Ledger Entry 2: RECEIVER CREDIT RECORD
        Transaction creditTx = new Transaction();
        creditTx.setAccountNumber(receiverAccount.getAccountNumber());
        creditTx.setPhoneNo(receiverAccount.getPhoneNo());
        creditTx.setTransactionType("AMOUNT_RECEIVED");
        creditTx.setAmount(request.getAmount());
        creditTx.setPostTransactionBalance(receiverAccount.getBalance());
        creditTx.setDescription("Received from A/C: " + senderAccount.getAccountNumber() + " (Ref: " + transactionRef + ")");
        creditTx.setTimestamp(LocalDateTime.now());
        transactionRepository.saveAndFlush(creditTx);

        return new FundTransferResponse(
                transactionRef,
                "Transfer of ₹" + request.getAmount() + " completed successfully.",
                senderAccount.getBalance()
        );
    }

    @Transactional
    public AccountHolder depositFunds(DepositRequest request) {
        if (request.getAmount() <= 0) {
            throw new RuntimeException("Deposit amount must be greater than zero.");
        }

        // Lookup the target account
        AccountHolder account = accountHolderRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account number not found."));




        // Add the cash to their current balance
        Double newBalance=account.getBalance()+request.getAmount();
        account.setBalance(newBalance);
        accountHolderRepository.save(account);

        // 2. NEW LEDGER PRINT: Create the transaction receipt row
        Transaction transaction = new Transaction();
        transaction.setAccountNumber(account.getAccountNumber());
        transaction.setTransactionType("DEPOSIT");
        transaction.setAmount(request.getAmount());
        transaction.setPostTransactionBalance(newBalance); // The calculated track summary
        transaction.setDescription("Cash counter deposit processed by Admin Officer.");
        transaction.setTimestamp(LocalDateTime.now());

        // Save receipt to database ledger
        transactionRepository.save(transaction);

        return account;
    }

    // Logic for normal verification
    public boolean verifyCustomerMpin(String username, String rawMpin) {
        User user = userRepository.findByIdentifier(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        AccountHolder account = accountHolderRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return passwordEncoder.matches(rawMpin, account.getMpin());
    }

    // Logic for resetting using account details (Secured by checking JWT ownership)
    @Transactional
    public void resetMpinWithAuthenticatedDetails(String username, MpinResetRequest request) {
        User user = userRepository.findByIdentifier(username)
                .orElseThrow(() -> new RuntimeException("Session invalid."));
        AccountHolder account = accountHolderRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Banking profile missing."));

        // Security Gate: Ensure they aren't trying to reset someone else's account number
        if (!account.getAccountNumber().equals(request.getAccountNumber())) {
            throw new RuntimeException("Access Denied: Account number does not match your logged-in profile.");
        }

        // Verify IFSC Code
        if (!account.getIfscCode().equalsIgnoreCase(request.getIfscCode())) {
            throw new RuntimeException("Verification Failed: Provided IFSC Code is incorrect.");
        }

        // Update and Save
        account.setMpin(passwordEncoder.encode(request.getNewMpin()));
        accountHolderRepository.saveAndFlush(account);
    }
}