package com.example.OnlineBanking.repository;

import com.example.OnlineBanking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByIdentifier(String identifier);
    Boolean existsByIdentifier(String identifier);

    Optional<User> findByResetToken(String resetToken);

}
