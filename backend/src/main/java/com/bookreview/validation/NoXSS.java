package com.bookreview.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = XSSValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoXSS {
    String message() default "입력에 XSS 위험 요소가 포함되어 있습니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}