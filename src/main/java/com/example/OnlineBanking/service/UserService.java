package com.example.OnlineBanking.service;

import com.example.OnlineBanking.dto.AdminSignupRequest;
import com.example.OnlineBanking.dto.LoginRequest;
import com.example.OnlineBanking.dto.MpinLoginRequest;
import com.example.OnlineBanking.dto.SignUpRequest;
import com.example.OnlineBanking.model.AccountHolder;
import com.example.OnlineBanking.model.User;
import com.example.OnlineBanking.repository.AccountHolderRepository;
import com.example.OnlineBanking.repository.UserRepository;
import java.util.UUID;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AccountHolderRepository accountHolderRepository;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, AccountHolderRepository accountHolderRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountHolderRepository = accountHolderRepository;
    }

    @Transactional
    public String registerUserCredentials(SignUpRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password Do Not Match");
        }

        if (userRepository.existsByIdentifier(request.getIdentifier())) {
            throw new RuntimeException("this mobile number or email has already registerd");
        }

        User newUser = new User();
        newUser.setIdentifier(request.getIdentifier());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole("ROLE_USER");

        userRepository.save(newUser);

        return "User Register Successfully ";

    }


    public User loginUser(LoginRequest request) {
        User user = userRepository.findByIdentifier(request.getIdentifier())
                .orElseThrow(() -> new RuntimeException("Invalid Mobile or Email"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password Try Again");
        }

        return user;
    }

    @Transactional(readOnly = true)
    public boolean verifyUserMpin(Long userid, String mpin) {
        // 1. Fetch the user profile from the database
        User user = userRepository.findById(userid)
                .orElseThrow(() -> new RuntimeException("User record not found."));

        // 2. Locate the linked account holder profile where the MPIN lives
        AccountHolder profile = accountHolderRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("No active banking account found for this user."));

        // 3. Match the raw 6-digit entry against the BCrypt hash stored in the DB
        if (!passwordEncoder.matches(mpin, profile.getMpin())) {
            throw new RuntimeException("Incorrect 6-digit MPIN. Please try again.");
        }

        return true; // Success!
    }

    @Transactional
    public User registerNewAdmin(AdminSignupRequest request) {
        // 1. Check if the email/identifier is already taken by a user or another admin
        if (userRepository.findByIdentifier(request.getIdentifier()).isPresent()) {
            throw new RuntimeException("Registration Failed:Admin Email is already registered in the system.");
        }

        if (request.getPassword() == null || request.getConfirmPassword() == null){
            throw new RuntimeException("{\"error\": \"Password and Confirm Password  fields cannot be empty.\"}");
        }

        if (!request.getPassword().equalsIgnoreCase(request.getConfirmPassword())){
            throw new RuntimeException("{\"error\": \"Validation Fault: The entered Password And ConfirmPassword do not match.\"}");
        }

        // 2. Create the new Admin User entity
        User admin = new User();
        admin.setIdentifier(request.getIdentifier());

        // 3. Hash the password securely using BCrypt before saving
        admin.setPassword(passwordEncoder.encode(request.getPassword()));

        // 4. Explicitly tag them with the Admin Role privilege flag
        admin.setRole("ROLE_ADMIN");

        // 5. Commit to MySQL database
        return userRepository.save(admin);
    }

    @Transactional
    public void generateForgotPasswordToken(String identifier) {
        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("Error: User with this email/identifier does not exist."));

        // 1. Generate a unique, secure string token
        String token = UUID.randomUUID().toString();

        // 2. Set expiry time to exactly 15 minutes from now
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(15);

        // 3. Save to User entity
        user.setResetToken(token);
        user.setResetTokenExpiry(expiryTime);
        userRepository.saveAndFlush(user); // Force instant DB write

        // 4. CHOICE B: Print simulated email link directly to the system console!
        System.out.println("\n=========================================================");
        System.out.println("🚨 [SIMULATED EMAIL SERVICE] SECURITY LOG");
        System.out.println("To reset your banking password, please copy the Token below:");
        System.out.println("TOKEN: " + token);
        System.out.println("Target API Route: POST /api/auth/reset-password");
        System.out.println("=========================================================\n");
    }

    @Transactional
    public void resetPasswordWithToken(String token, String newPassword) {
        // 1. Look up user by the token
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Error: Invalid or broken password reset token."));

        // 2. Security Rule: Check if token has expired
        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Error: This password reset token has expired. Please request a new link.");
        }

        // 3. Update with securely hashed new password
        user.setPassword(passwordEncoder.encode(newPassword));

        // 4. CRITICAL: Clear the token fields so the link can NEVER be used a second time!
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.saveAndFlush(user);
    }
}
