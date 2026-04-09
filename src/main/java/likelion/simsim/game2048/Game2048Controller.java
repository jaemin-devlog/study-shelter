package likelion.simsim.game2048;

import likelion.simsim.auth.SessionService;
import likelion.simsim.auth.SessionTokenResolver;
import likelion.simsim.auth.model.SessionInfo;
import likelion.simsim.common.ApiResponse;
import likelion.simsim.common.AuthConstants;
import likelion.simsim.game2048.dto.Game2048RankingResponse;
import likelion.simsim.game2048.dto.Game2048ScoreRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/2048")
public class Game2048Controller {

    private final Game2048Service game2048Service;
    private final SessionTokenResolver sessionTokenResolver;
    private final SessionService sessionService;

    public Game2048Controller(
            Game2048Service game2048Service,
            SessionTokenResolver sessionTokenResolver,
            SessionService sessionService
    ) {
        this.game2048Service = game2048Service;
        this.sessionTokenResolver = sessionTokenResolver;
        this.sessionService = sessionService;
    }

    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<Game2048RankingResponse>> ranking(
            @RequestHeader(value = AuthConstants.AUTHORIZATION_HEADER, required = false) String authorizationHeader,
            @RequestHeader(value = AuthConstants.SESSION_TOKEN_HEADER, required = false) String sessionTokenHeader
    ) {
        String sessionToken = sessionTokenResolver.resolve(authorizationHeader, sessionTokenHeader);
        String nickname = sessionToken == null
                ? null
                : sessionService.findSession(sessionToken).map(SessionInfo::nickname).orElse(null);

        return ResponseEntity.ok(ApiResponse.ok(
                "2048 점수 랭킹을 조회했습니다.",
                game2048Service.getRanking(nickname)
        ));
    }

    @PostMapping("/score")
    public ResponseEntity<ApiResponse<Game2048RankingResponse>> saveScore(
            @RequestHeader(value = AuthConstants.AUTHORIZATION_HEADER, required = false) String authorizationHeader,
            @RequestHeader(value = AuthConstants.SESSION_TOKEN_HEADER, required = false) String sessionTokenHeader,
            @RequestBody Game2048ScoreRequest request
    ) {
        String sessionToken = sessionTokenResolver.resolveOrThrow(authorizationHeader, sessionTokenHeader);
        SessionInfo sessionInfo = sessionService.requireSession(sessionToken);

        return ResponseEntity.ok(ApiResponse.ok(
                "2048 최고 점수를 저장했습니다.",
                game2048Service.saveScore(sessionInfo, request.score())
        ));
    }

    @PostMapping("/share")
    public ResponseEntity<ApiResponse<Game2048RankingResponse>> shareScore(
            @RequestHeader(value = AuthConstants.AUTHORIZATION_HEADER, required = false) String authorizationHeader,
            @RequestHeader(value = AuthConstants.SESSION_TOKEN_HEADER, required = false) String sessionTokenHeader,
            @RequestBody Game2048ScoreRequest request
    ) {
        String sessionToken = sessionTokenResolver.resolveOrThrow(authorizationHeader, sessionTokenHeader);
        SessionInfo sessionInfo = sessionService.requireSession(sessionToken);

        return ResponseEntity.ok(ApiResponse.ok(
                "2048 점수를 채팅방에 공유했습니다.",
                game2048Service.shareScore(sessionInfo, request.score())
        ));
    }
}
