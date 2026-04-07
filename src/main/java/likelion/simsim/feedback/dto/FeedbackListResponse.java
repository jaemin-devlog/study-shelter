package likelion.simsim.feedback.dto;

import java.util.List;

public record FeedbackListResponse(
        List<FeedbackPostResponse> posts
) {
}
