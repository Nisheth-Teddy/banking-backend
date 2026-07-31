package com.example.OnlineBanking.repository;

import com.example.OnlineBanking.model.AccountHolder;
import com.example.OnlineBanking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountHolderRepository extends JpaRepository<AccountHolder,Long> {
    Optional<AccountHolder> findByUser(User user);
    Optional<AccountHolder>findByAccountNumber(String accountNumber);
    Optional<AccountHolder>findByPhoneNo(String phoneNo);


}
