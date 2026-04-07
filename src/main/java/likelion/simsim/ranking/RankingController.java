package likelion.simsim.ranking;

import likelion.simsim.common.ApiResponse;
import likelion.simsim.ranking.dto.RankingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상위 10명 랭킹 조회용 REST API 입니다.
 */
@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/top10")
    public ResponseEntity<ApiResponse<RankingResponse>> top10() {
        return ResponseEntity.ok(ApiResponse.ok("현재 학업대피소 랭킹 Top 10을 조회했습니다.", rankingService.getTop10()));
    }
}
