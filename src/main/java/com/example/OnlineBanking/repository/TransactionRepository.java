package com.example.OnlineBanking.repository;

import com.example.OnlineBanking.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Fetches the entire passbook statement for an account, ordered by newest transactions first!
    List<Transaction> findByAccountNumberOrderByTimestampDesc(String accountNumber);
}