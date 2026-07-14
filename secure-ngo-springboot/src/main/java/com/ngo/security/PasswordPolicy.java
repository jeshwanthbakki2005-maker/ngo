package com.ngo.security;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    public void validate(String password) {
        if (password == null || password.length() < 12)
            throw new IllegalArgumentException("Password must contain at least 12 characters.");
        if (password.length() > 72)
            throw new IllegalArgumentException("Password must not exceed 72 characters.");
        if (!password.matches(".*[A-Z].*"))
            throw new IllegalArgumentException("Password must include an uppercase letter.");
        if (!password.matches(".*[a-z].*"))
            throw new IllegalArgumentException("Password must include a lowercase letter.");
        if (!password.matches(".*[0-9].*"))
            throw new IllegalArgumentException("Password must include a number.");
        if (!password.matches(".*[^A-Za-z0-9].*"))
            throw new IllegalArgumentException("Password must include a special character.");
        if (password.matches(".*\\s.*"))
            throw new IllegalArgumentException("Password must not contain spaces.");
    }
}
