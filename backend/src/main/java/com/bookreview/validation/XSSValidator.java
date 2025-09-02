package com.bookreview.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class XSSValidator implements ConstraintValidator<NoXSS, String> {

    // XSS 위험 패턴들
    private static final Pattern[] XSS_PATTERNS = {
        // 스크립트 태그
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
        
        // 자바스크립트 이벤트 핸들러
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        
        // 자바스크립트 URL
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        
        // iframe 태그
        Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
        
        // object, embed 태그
        Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
        
        // 폼 태그
        Pattern.compile("<form[^>]*>", Pattern.CASE_INSENSITIVE),
        
        // 메타 태그
        Pattern.compile("<meta[^>]*>", Pattern.CASE_INSENSITIVE),
        
        // 링크 태그
        Pattern.compile("<link[^>]*>", Pattern.CASE_INSENSITIVE),
        
        // 스타일 태그
        Pattern.compile("<style[^>]*>.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        
        // 위험한 CSS
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("@import", Pattern.CASE_INSENSITIVE),
        
        // HTML 엔티티를 통한 우회 시도
        Pattern.compile("&#x?[0-9a-f]+;?", Pattern.CASE_INSENSITIVE),
        
        // SQL Injection 패턴
        Pattern.compile("(union|select|insert|update|delete|drop|create|alter|exec|execute)\\s", Pattern.CASE_INSENSITIVE),
        
        // 기타 위험한 패턴
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("document\\.cookie", Pattern.CASE_INSENSITIVE),
        Pattern.compile("document\\.write", Pattern.CASE_INSENSITIVE),
        Pattern.compile("window\\.location", Pattern.CASE_INSENSITIVE)
    };

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // null/empty는 다른 어노테이션에서 처리
        }
        
        // XSS 패턴 검사
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                addConstraintViolation(context, "보안상 위험한 내용이 포함되어 있습니다");
                return false;
            }
        }
        
        // 연속된 특수문자 확인 (잠재적 우회 시도)
        if (hasConsecutiveSpecialChars(value, 5)) {
            addConstraintViolation(context, "연속된 특수문자가 너무 많습니다");
            return false;
        }
        
        return true;
    }
    
    private boolean hasConsecutiveSpecialChars(String value, int maxConsecutive) {
        int consecutiveCount = 0;
        
        for (char c : value.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                consecutiveCount++;
                if (consecutiveCount >= maxConsecutive) {
                    return true;
                }
            } else {
                consecutiveCount = 0;
            }
        }
        
        return false;
    }
    
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}