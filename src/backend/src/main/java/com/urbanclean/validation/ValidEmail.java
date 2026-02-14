package com.urbanclean.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Annotation for RFC 5322 compliant email validation
 */
@Documented
@Constraint(validatedBy = EmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {
    
    String message() default "Email format is invalid";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
