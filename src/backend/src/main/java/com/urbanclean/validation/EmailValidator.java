package com.urbanclean.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Validator for email format compliance with RFC 5322
 */
@Slf4j
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    // RFC 5322 compliant email regex (simplified but comprehensive)
    // This pattern validates most common email formats while being RFC 5322 compliant
    private static final String RFC5322_EMAIL_PATTERN = 
        "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(RFC5322_EMAIL_PATTERN);

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Email cannot be empty")
                   .addConstraintViolation();
            return false;
        }

        // Check basic format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Email format is invalid")
                   .addConstraintViolation();
            return false;
        }

        // Additional validations
        String[] parts = email.split("@");
        if (parts.length != 2) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Email must contain exactly one @ symbol")
                   .addConstraintViolation();
            return false;
        }

        String localPart = parts[0];
        String domainPart = parts[1];

        // Validate local part length (max 64 characters per RFC 5322)
        if (localPart.length() > 64) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Email local part is too long (max 64 characters)")
                   .addConstraintViolation();
            return false;
        }

        // Validate domain part length (max 255 characters per RFC 5322)
        if (domainPart.length() > 255) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Email domain is too long (max 255 characters)")
                   .addConstraintViolation();
            return false;
        }

        // Check for consecutive dots
        if (email.contains("..")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Email cannot contain consecutive dots")
                   .addConstraintViolation();
            return false;
        }

        // Check if starts or ends with dot
        if (localPart.startsWith(".") || localPart.endsWith(".")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Email local part cannot start or end with a dot")
                   .addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Static method for programmatic validation
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false;
        }

        String localPart = parts[0];
        String domainPart = parts[1];

        if (localPart.length() > 64 || domainPart.length() > 255) {
            return false;
        }

        if (email.contains("..") || localPart.startsWith(".") || localPart.endsWith(".")) {
            return false;
        }

        return true;
    }
}
