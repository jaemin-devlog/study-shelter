package likelion.simsim.feedback;

import likelion.simsim.common.exception.BadRequestException;
import likelion.simsim.feedback.dto.FeedbackCreateRequest;
import likelion.simsim.feedback.dto.FeedbackListResponse;
import likelion.simsim.feedback.dto.FeedbackPostResponse;
import likelion.simsim.feedback.entity.FeedbackPostEntity;
import likelion.simsim.feedback.repository.FeedbackPostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class FeedbackService {

    private static final String DEFAULT_TITLE = "피드백";
    private static final int MAX_POST_COUNT = 100;

    private final FeedbackPostRepository feedbackPostRepository;

    public FeedbackService(FeedbackPostRepository feedbackPostRepository) {
        this.feedbackPostRepository = feedbackPostRepository;
    }

    public FeedbackPostResponse create(FeedbackCreateRequest request) {
        String nickname = normalizeRequired(request.nickname(), "닉네임을 입력하세요.");
        String school = normalizeSchool(request.school());
        String content = normalizeRequired(request.content(), "내용을 입력하세요.");

        if (nickname.length() > 20) {
            throw new BadRequestException("닉네임은 20자 이하로 입력하세요.");
        }
        if (school.length() > 40) {
            throw new BadRequestException("학교명은 40자 이하로 입력하세요.");
        }
        if (content.length() > 1000) {
            throw new BadRequestException("내용은 1000자 이하로 입력하세요.");
        }

        FeedbackPostEntity entity = feedbackPostRepository.save(new FeedbackPostEntity(
                HtmlUtils.htmlEscape(nickname),
                HtmlUtils.htmlEscape(school),
                DEFAULT_TITLE,
                HtmlUtils.htmlEscape(content),
                System.currentTimeMillis()
        ));

        return toResponse(entity);
    }

    public FeedbackListResponse getRecentPosts() {
        List<FeedbackPostEntity> entities = feedbackPostRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, MAX_POST_COUNT));
        List<FeedbackPostResponse> posts = new ArrayList<>();

        for (FeedbackPostEntity entity : entities) {
            posts.add(toResponse(entity));
        }

        return new FeedbackListResponse(posts);
    }

    private FeedbackPostResponse toResponse(FeedbackPostEntity entity) {
        return new FeedbackPostResponse(
                entity.getId(),
                entity.getNickname(),
                entity.getSchool(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new BadRequestException(message);
        }
        return normalized;
    }

    private String normalizeSchool(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? "학교 미입력" : normalized;
    }
}
