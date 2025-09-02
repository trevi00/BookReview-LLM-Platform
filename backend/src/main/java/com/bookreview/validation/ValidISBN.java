package com.bookreview.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ISBNValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidISBN {
    String message() default "유효하지 않은 ISBN 형식입니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}