package likelion.simsim.chat;

import likelion.simsim.chat.dto.RecentChatResponse;
import likelion.simsim.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 최근 채팅 이력 조회용 REST API 입니다.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;

    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<RecentChatResponse>> recent() {
        return ResponseEntity.ok(ApiResponse.ok("최근 채팅 기록을 조회했습니다.", chatService.getRecentMessages()));
    }
}
