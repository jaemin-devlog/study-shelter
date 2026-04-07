package likelion.simsim.presence;

import likelion.simsim.common.ApiResponse;
import likelion.simsim.presence.dto.OnlineCountResponse;
import likelion.simsim.presence.dto.OnlineUsersResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현재 온라인 인원 조회용 REST API 입니다.
 */
@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<OnlineCountResponse>> count() {
        return ResponseEntity.ok(ApiResponse.ok("현재 접속 중 인원을 조회했습니다.", presenceService.getOnlineCount()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<OnlineUsersResponse>> users() {
        return ResponseEntity.ok(ApiResponse.ok("현재 접속 명단을 조회했습니다.", presenceService.getOnlineUsers()));
    }
}
