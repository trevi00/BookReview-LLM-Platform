package com.bookreview.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ISBNValidator implements ConstraintValidator<ValidISBN, String> {

    @Override
    public boolean isValid(String isbn, ConstraintValidatorContext context) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return true; // null/empty는 다른 어노테이션에서 처리
        }
        
        // ISBN 정규화 (하이픈 제거)
        String normalizedIsbn = isbn.replaceAll("[\\s-]", "");
        
        // ISBN-10 또는 ISBN-13 형식 확인
        return isValidISBN10(normalizedIsbn) || isValidISBN13(normalizedIsbn);
    }
    
    private boolean isValidISBN10(String isbn) {
        if (isbn.length() != 10) {
            return false;
        }
        
        try {
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                char c = isbn.charAt(i);
                if (!Character.isDigit(c)) {
                    return false;
                }
                sum += (c - '0') * (10 - i);
            }
            
            char checkChar = isbn.charAt(9);
            int checkDigit;
            if (checkChar == 'X' || checkChar == 'x') {
                checkDigit = 10;
            } else if (Character.isDigit(checkChar)) {
                checkDigit = checkChar - '0';
            } else {
                return false;
            }
            
            sum += checkDigit;
            return sum % 11 == 0;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidISBN13(String isbn) {
        if (isbn.length() != 13) {
            return false;
        }
        
        // ISBN-13은 978 또는 979로 시작해야 함
        if (!isbn.startsWith("978") && !isbn.startsWith("979")) {
            return false;
        }
        
        try {
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                char c = isbn.charAt(i);
                if (!Character.isDigit(c)) {
                    return false;
                }
                int digit = c - '0';
                sum += (i % 2 == 0) ? digit : digit * 3;
            }
            
            char checkChar = isbn.charAt(12);
            if (!Character.isDigit(checkChar)) {
                return false;
            }
            
            int checkDigit = checkChar - '0';
            int calculatedCheckDigit = (10 - (sum % 10)) % 10;
            
            return checkDigit == calculatedCheckDigit;
            
        } catch (Exception e) {
            return false;
        }
    }
}