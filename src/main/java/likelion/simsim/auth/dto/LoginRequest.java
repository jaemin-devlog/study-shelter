package likelion.simsim.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 12, message = "닉네임은 2자 이상 12자 이하로 입력하세요.")
        String nickname,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 4, max = 30, message = "비밀번호는 4자 이상 30자 이하로 입력하세요.")
        String password
) {
}
