package likelion.simsim.feedback;

import jakarta.validation.Valid;
import likelion.simsim.common.ApiResponse;
import likelion.simsim.feedback.dto.FeedbackCreateRequest;
import likelion.simsim.feedback.dto.FeedbackListResponse;
import likelion.simsim.feedback.dto.FeedbackPostResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<FeedbackListResponse>> posts() {
        return ResponseEntity.ok(ApiResponse.ok("피드백 게시글을 조회했습니다.", feedbackService.getRecentPosts()));
    }

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<FeedbackPostResponse>> create(@Valid @RequestBody FeedbackCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("피드백이 등록되었습니다.", feedbackService.create(request)));
    }
}
