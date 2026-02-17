package com.urbanclean.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validator for password complexity requirements
 * Requirements:
 * - Minimum 8 characters
 * - At least 1 uppercase letter
 * - At least 1 lowercase letter
 * - At least 1 number
 * - At least 1 special character
 * - Cannot contain username or email
 * - Not in common password blacklist
 */
@Slf4j
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // Top 100 most common passwords (subset for demonstration)
    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
        "password", "123456", "12345678", "qwerty", "abc123", "monkey", "1234567",
        "letmein", "trustno1", "dragon", "baseball", "111111", "iloveyou", "master",
        "sunshine", "ashley", "bailey", "passw0rd", "shadow", "123123", "654321",
        "superman", "qazwsx", "michael", "football", "welcome", "jesus", "ninja",
        "mustang", "password1", "123456789", "adobe123", "admin", "1234567890",
        "photoshop", "1234", "12345", "password123", "welcome123", "admin123"
    ));

    private String username;
    private String email;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            addViolation(context, "Password cannot be empty");
            return false;
        }

        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        // Check minimum length
        if (password.length() < 8) {
            addViolation(context, "Password must be at least 8 characters long");
            isValid = false;
        }

        // Check for uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            addViolation(context, "Password must contain at least one uppercase letter");
            isValid = false;
        }

        // Check for lowercase letter
        if (!password.matches(".*[a-z].*")) {
            addViolation(context, "Password must contain at least one lowercase letter");
            isValid = false;
        }

        // Check for digit
        if (!password.matches(".*\\d.*")) {
            addViolation(context, "Password must contain at least one number");
            isValid = false;
        }

        // Check for special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            addViolation(context, "Password must contain at least one special character");
            isValid = false;
        }

        // Check if password is in common passwords list
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            addViolation(context, "Password is too common. Please choose a more secure password");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Validate password doesn't contain username or email
     * This should be called separately with context
     */
    public boolean validateNotContainsUserInfo(String password, String username, String email, 
                                               ConstraintValidatorContext context) {
        boolean isValid = true;

        if (username != null && !username.isEmpty() && 
            password.toLowerCase().contains(username.toLowerCase())) {
            addViolation(context, "Password cannot contain your username");
            isValid = false;
        }

        if (email != null && !email.isEmpty()) {
            String emailPrefix = email.split("@")[0];
            if (password.toLowerCase().contains(emailPrefix.toLowerCase())) {
                addViolation(context, "Password cannot contain your email address");
                isValid = false;
            }
        }

        return isValid;
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }

    /**
     * Static method for programmatic validation
     */
    public static ValidationResult validate(String password, String username, String email) {
        ValidationResult result = new ValidationResult();

        if (password == null || password.isEmpty()) {
            result.addError("Password cannot be empty");
            return result;
        }

        if (password.length() < 8) {
            result.addError("Password must be at least 8 characters long");
        }

        if (!password.matches(".*[A-Z].*")) {
            result.addError("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            result.addError("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            result.addError("Password must contain at least one number");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            result.addError("Password must contain at least one special character");
        }

        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            result.addError("Password is too common. Please choose a more secure password");
        }

        if (username != null && !username.isEmpty() && 
            password.toLowerCase().contains(username.toLowerCase())) {
            result.addError("Password cannot contain your username");
        }

        if (email != null && !email.isEmpty()) {
            String emailPrefix = email.split("@")[0];
            if (password.toLowerCase().contains(emailPrefix.toLowerCase())) {
                result.addError("Password cannot contain your email address");
            }
        }

        return result;
    }

    /**
     * Result class for validation
     */
    public static class ValidationResult {
        private final Set<String> errors = new HashSet<>();

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public Set<String> getErrors() {
            return errors;
        }
    }
}
