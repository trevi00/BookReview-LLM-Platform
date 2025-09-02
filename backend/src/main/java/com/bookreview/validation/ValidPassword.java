package com.bookreview.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "비밀번호는 8자 이상, 대문자, 소문자, 숫자, 특수문자를 포함해야 합니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}