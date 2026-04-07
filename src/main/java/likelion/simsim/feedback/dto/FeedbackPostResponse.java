package likelion.simsim.feedback.dto;

public record FeedbackPostResponse(
        Long id,
        String nickname,
        String school,
        String content,
        long createdAt
) {
}
