package com.example.OnlineBanking.controller;

import com.example.OnlineBanking.dto.*;
import com.example.OnlineBanking.model.AccountHolder;
import com.example.OnlineBanking.model.User;
import com.example.OnlineBanking.repository.AccountHolderRepository;
import com.example.OnlineBanking.repository.UserRepository;
import com.example.OnlineBanking.security.JwtUtils;
import com.example.OnlineBanking.service.AccountHolderService;
import com.example.OnlineBanking.service.UserService;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bank/auth")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final AccountHolderRepository accountHolderRepository;


    public UserController(UserService userService, UserRepository userRepository, JwtUtils jwtUtils, AccountHolderRepository accountHolderRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.accountHolderRepository = accountHolderRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequest request) {
        try {
            String response = userService.registerUserCredentials(request);
            return ResponseEntity.ok().body(Map.of("message", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("Error", e.getMessage()));
        }
    }

    // A secret key that only bank IT staff know. In production, this lives in application.properties
    private final String SECRET_BANK_PASSKEY = "DBI_SSK_2026";

    @PostMapping("/admin/signup")
    public ResponseEntity<?> registerAdmin(@RequestBody AdminSignupRequest request) {
        try {
            // 1. Verify if the employee knows the bank's master passkey
            if (!SECRET_BANK_PASSKEY.equals(request.getSecretAdminKey())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access Denied: Invalid Bank Registration Passkey."));
            }

            // 2. Pass the data to the service layer to save as ROLE_ADMIN
            User newAdmin = userService.registerNewAdmin(request);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Admin Employee account registered successfully!",
                    "adminId", newAdmin.getUserId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User loggedInUser = userService.loginUser(request);

            // 1. Core Profile Rule: Admins automatically bypass the MPIN step
            boolean initialMpinStatus = loggedInUser.getRole().equals("ROLE_ADMIN");

            // 2. Generate initial tokens
            String accessToken = jwtUtils.generateAccessToken(loggedInUser.getIdentifier(), loggedInUser.getRole(), initialMpinStatus);
            String refreshToken = jwtUtils.generateRefreshToken(loggedInUser.getIdentifier());

            // 3. Database Check: Does this user actually have an account profile row?
            Optional<AccountHolder> holderProfile = accountHolderRepository.findByUser(loggedInUser);

            // 🌟 Initialize our brand new unified AuthResponse DTO
            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(loggedInUser.getUserId()) // 🔥 Crucial: Returns the hidden database ID for ALL scenarios!
                    .build();

            // CASE 1: USER IS AN ADMIN EMPLOYEE
            if (loggedInUser.getRole().equals("ROLE_ADMIN")) {
                return ResponseEntity.ok().body(Map.of(
                        "accessToken", accessToken,
                        "refreshToken", refreshToken,
                        "role", loggedInUser.getRole(),
                        "targetPage", "ADMIN_DASHBOARD",
                        "message", "Admin clearance granted. Welcome to Command Center."
                ));
            }

            // CASE 2: USER IS A REGULAR CUSTOMER
            else {
                if (holderProfile.isPresent()) {
                    // Customer exists and already has an account configured -> Send to MPIN Wall
                    return ResponseEntity.ok().body(Map.of(
                            "accessToken", accessToken,
                            "refreshToken", refreshToken,
                            "role", loggedInUser.getRole(),
                            "targetPage", "MPIN_SECURITY_CHECKPOINT",
                            "message", "Authentication successful. Please enter your 6-digit MPIN to unlock your dashboard."
                    ));
                } else {
                    // Brand New Customer! No banking profile found -> Route them to Account Opening Form!
                    return ResponseEntity.ok().body(Map.of(
                            "accessToken", accessToken,
                            "refreshToken", refreshToken,
                            "role", loggedInUser.getRole(),
                            "targetPage", "OPEN_ACCOUNT_FORM",
                            "userId", loggedInUser.getUserId(),
                            "identifier", loggedInUser.getIdentifier(),
                            "message", "Welcome! Please initialize your new digital banking profile and configure your 6-digit MPIN."
                    ));
                }
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-mpin")
    public ResponseEntity<?> verifyMpin(@RequestBody Map<String, String> request) {
        try {
            String mpin = request.get("mpin");

            // 1. Extract what name Spring Security processed from the JWT
            String principalName = SecurityContextHolder.getContext().getAuthentication().getName();


            // 2. Try to find the user by their identifier
            User loggedInUser = userRepository.findByIdentifier(principalName)
                    .orElseThrow(() -> new RuntimeException("User authorization record not found for: " + principalName));

            // 3. Verify the MPIN using your core user authentication service
            boolean isValid = userService.verifyUserMpin(loggedInUser.getUserId(), mpin);

            if (isValid) {
                // 4. Generate the elevated session token with isMpinVerified = true
                String elevatedAccessToken = jwtUtils.generateAccessToken(loggedInUser.getIdentifier(), loggedInUser.getRole(), true);

                return ResponseEntity.ok().body(Map.of(
                        "accessToken", elevatedAccessToken,
                        "targetPage", "DASHBOARD",
                        "message", "Welcome to your cockpit!"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid MPIN"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> handleForgotPassword(@RequestBody Map<String, String> request) {
        try {
            String identifier = request.get("identifier");
            userService.generateForgotPasswordToken(identifier);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "If the account exists, a secure recovery code has been processed. Check the server console logs!"
            ));
        } catch (Exception e) {
            // Bank Security Best Practice: Don't tell hackers if an email exists or not.
            // But for our testing convenience, we return the real error message:
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> handleResetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");

            userService.resetPasswordWithToken(token, newPassword);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Your banking password has been successfully updated! You can now log in with your new credentials."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is missing."));
            }

            // 1. Validate the refresh token using your JwtUtils
            if (jwtUtils.isTokenValid(refreshToken)) { // Note: replace with your exact validation method if named differently
                String identifier = jwtUtils.extractIdentifier(refreshToken); // or extractUsername

                // 2. Fetch user details to look up their current role
                User user = userRepository.findByIdentifier(identifier)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                // 3. Generate a brand new short-lived Access Token

                String newAccessToken = jwtUtils.generateAccessToken(user.getIdentifier(), user.getRole(), false);

                return ResponseEntity.ok(Map.of(
                        "accessToken", newAccessToken
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or Expired Refresh Token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }



}

