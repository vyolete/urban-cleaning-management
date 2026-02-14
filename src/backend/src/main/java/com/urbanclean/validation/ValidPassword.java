package com.urbanclean.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Annotation for password complexity validation
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    
    String message() default "Password does not meet complexity requirements";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
