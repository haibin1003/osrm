package com.osrm;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("admin123");
        System.out.println("========================================");
        System.out.println("BCrypt hash for 'admin123':");
        System.out.println(hash);
        System.out.println("========================================");
        System.out.println("Verification: " + encoder.matches("admin123", hash));
    }
}
