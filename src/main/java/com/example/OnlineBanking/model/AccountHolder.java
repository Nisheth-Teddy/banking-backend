package com.example.OnlineBanking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "account_holders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_holder_id")
    private Long id;

    // Connects this extensive profile directly to their login credentials
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false,length = 10)
    private String phoneNo;

    @Column(unique = true, nullable = false,length = 15)
    private String identityCardNumber; // Stores official national identity securely

    @Column(nullable = false)
    private String parentName;

    @Column(nullable = false,unique = true,length = 10)
    private String parentPhoneNo;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "mpin",nullable = false,unique = true)
    private String mpin; // Encrypted 6-digit MPIN stays with the active bank profile

    @Column(nullable = false,unique = true)
    private String accountNumber;

    @Column(nullable = false,unique = true)
    private  String ifscCode;

    @Column(nullable = false)
    private  Double balance;
}