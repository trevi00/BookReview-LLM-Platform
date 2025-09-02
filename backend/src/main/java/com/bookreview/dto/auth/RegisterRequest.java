package com.bookreview.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&].*$",
        message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다"
    )
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String confirmPassword;

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 2, max = 50, message = "사용자명은 2자 이상 50자 이하여야 합니다")
    @Pattern(
        regexp = "^[가-힣a-zA-Z0-9_-]+$",
        message = "사용자명은 한글, 영문, 숫자, 언더스코어, 하이픈만 사용할 수 있습니다"
    )
    private String username;

    @AssertTrue(message = "비밀번호가 일치하지 않습니다")
    public boolean isPasswordMatching() {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }
}