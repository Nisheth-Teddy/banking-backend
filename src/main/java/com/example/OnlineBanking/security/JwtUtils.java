package com.example.OnlineBanking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtils {

    private static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Lifespans
    private static final long ACCESS_TOKEN_VALIDITY = 15 * 60 * 1000L;       // 15 Minutes (Safe!)
    private static final long REFRESH_TOKEN_VALIDITY = 30L * 24 * 60 * 60 * 1000L; // 30 Days

    // Generate Short-Lived Access Token with an MPIN status flag
    public String generateAccessToken(String identifier, String role, boolean isMpinVerified) {
        return Jwts.builder()
                .setSubject(identifier)
                .claim("role", role)
                .claim("isMpinVerified", isMpinVerified) // <-- THE CRITICAL SECURITY FLAG
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }

    // Generate Long-Lived Refresh Token (Minimal data for safety)
    public String generateRefreshToken(String identifier) {
        return Jwts.builder()
                .setSubject(identifier)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }

    // Extraction helper for our new flag
    public boolean extractMpinVerificationStatus(String token) {
        return extractAllClaims(token).get("isMpinVerified", Boolean.class);
    }

    // 4. EXTRACTION: Pull the username/email back out of a token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    // METHOD 1: For Standard Access Tokens (Requires Username Matching)

    public boolean isTokenValid(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token); // or extractIdentifier
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }


    // METHOD 2: Overloaded version for Refresh Tokens (Only checks expiration)

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false; // Returns false if token is tampered with or expired
        }
    }

    // --- Private Helper Methods for Cryptographic Parsing ---

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractIdentifier(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }


}