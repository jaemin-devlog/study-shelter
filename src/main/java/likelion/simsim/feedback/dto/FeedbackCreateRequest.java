package likelion.simsim.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackCreateRequest(
        @NotBlank(message = "닉네임을 입력하세요.")
        @Size(max = 20, message = "닉네임은 20자 이하로 입력하세요.")
        String nickname,

        @Size(max = 40, message = "학교명은 40자 이하로 입력하세요.")
        String school,

        @NotBlank(message = "내용을 입력하세요.")
        @Size(max = 1000, message = "내용은 1000자 이하로 입력하세요.")
        String content
) {
}
