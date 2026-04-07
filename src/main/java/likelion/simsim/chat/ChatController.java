package likelion.simsim.chat;

import likelion.simsim.auth.SessionService;
import likelion.simsim.auth.model.SessionInfo;
import likelion.simsim.chat.dto.ChatMessageRequest;
import likelion.simsim.common.AuthConstants;
import likelion.simsim.common.exception.UnauthorizedException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 전체 익명 채팅방 메시지 발행을 처리합니다.
 */
@Controller
public class ChatController {

    private final ChatService chatService;
    private final SessionService sessionService;

    public ChatController(ChatService chatService, SessionService sessionService) {
        this.chatService = chatService;
        this.sessionService = sessionService;
    }

    @MessageMapping("/chat.send")
    public void send(ChatMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionToken = resolveSessionToken(headerAccessor);
        SessionInfo sessionInfo = sessionService.requireSession(sessionToken);
        chatService.sendChat(sessionInfo.nickname(), sessionInfo.school(), request.content());
    }

    private String resolveSessionToken(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new UnauthorizedException("세션 속성이 없습니다.");
        }

        Object tokenValue = sessionAttributes.get(AuthConstants.SESSION_TOKEN_ATTR);
        String sessionToken = tokenValue == null ? null : String.valueOf(tokenValue);
        if (!StringUtils.hasText(sessionToken)) {
            throw new UnauthorizedException("세션 토큰이 없습니다.");
        }
        return sessionToken;
    }
}
