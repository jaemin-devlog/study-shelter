package likelion.simsim.ranking;

import likelion.simsim.common.ApiResponse;
import likelion.simsim.ranking.dto.MemberDirectoryResponse;
import likelion.simsim.ranking.dto.RankingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/top10")
    public ResponseEntity<ApiResponse<RankingResponse>> top10() {
        return ResponseEntity.ok(ApiResponse.ok("현재 학업대피소 전체 누적 Top 10을 조회했습니다.", rankingService.getTop10()));
    }

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<MemberDirectoryResponse>> members(
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(ApiResponse.ok("전체 가입자 명단을 조회했습니다.", rankingService.getMemberDirectory(page, 15)));
    }
}
