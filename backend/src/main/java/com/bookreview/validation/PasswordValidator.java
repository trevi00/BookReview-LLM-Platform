package com.bookreview.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    private static final Pattern COMMON_PATTERN = Pattern.compile("(password|123456|qwerty|admin)", Pattern.CASE_INSENSITIVE);
    
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.trim().isEmpty()) {
            return true; // null/empty는 다른 어노테이션에서 처리
        }
        
        // 길이 확인
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            addConstraintViolation(context, String.format("비밀번호는 %d자 이상 %d자 이하여야 합니다", MIN_LENGTH, MAX_LENGTH));
            return false;
        }
        
        // 대문자 포함 확인
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            addConstraintViolation(context, "비밀번호에 대문자를 포함해야 합니다");
            return false;
        }
        
        // 소문자 포함 확인
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            addConstraintViolation(context, "비밀번호에 소문자를 포함해야 합니다");
            return false;
        }
        
        // 숫자 포함 확인
        if (!DIGIT_PATTERN.matcher(password).find()) {
            addConstraintViolation(context, "비밀번호에 숫자를 포함해야 합니다");
            return false;
        }
        
        // 특수문자 포함 확인
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            addConstraintViolation(context, "비밀번호에 특수문자를 포함해야 합니다");
            return false;
        }
        
        // 일반적인 비밀번호 패턴 확인
        if (COMMON_PATTERN.matcher(password).find()) {
            addConstraintViolation(context, "일반적인 비밀번호는 사용할 수 없습니다");
            return false;
        }
        
        // 연속된 문자 확인 (3개 이상)
        if (hasConsecutiveChars(password, 3)) {
            addConstraintViolation(context, "3개 이상의 연속된 문자는 사용할 수 없습니다");
            return false;
        }
        
        // 반복된 문자 확인 (3개 이상)
        if (hasRepeatedChars(password, 3)) {
            addConstraintViolation(context, "같은 문자를 3개 이상 연속으로 사용할 수 없습니다");
            return false;
        }
        
        return true;
    }
    
    private boolean hasConsecutiveChars(String password, int maxConsecutive) {
        for (int i = 0; i <= password.length() - maxConsecutive; i++) {
            boolean isConsecutive = true;
            for (int j = 1; j < maxConsecutive; j++) {
                if (password.charAt(i + j) != password.charAt(i) + j) {
                    isConsecutive = false;
                    break;
                }
            }
            if (isConsecutive) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasRepeatedChars(String password, int maxRepeated) {
        for (int i = 0; i <= password.length() - maxRepeated; i++) {
            boolean isRepeated = true;
            char baseChar = password.charAt(i);
            for (int j = 1; j < maxRepeated; j++) {
                if (password.charAt(i + j) != baseChar) {
                    isRepeated = false;
                    break;
                }
            }
            if (isRepeated) {
                return true;
            }
        }
        return false;
    }
    
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}