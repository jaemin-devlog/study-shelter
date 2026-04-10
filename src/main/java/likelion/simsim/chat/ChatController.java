package likelion.simsim.chat;

import likelion.simsim.auth.SessionService;
import likelion.simsim.auth.model.SessionInfo;
import likelion.simsim.chat.dto.ChatAnnouncementRequest;
import likelion.simsim.chat.dto.ChatMessageRequest;
import likelion.simsim.common.AuthConstants;
import likelion.simsim.common.exception.UnauthorizedException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.util.Map;

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
        SessionInfo sessionInfo = sessionService.requireSession(resolveSessionToken(headerAccessor));
        chatService.sendChat(sessionInfo.nickname(), sessionInfo.school(), request.content());
    }

    @MessageMapping("/chat.announce")
    public void announce(ChatAnnouncementRequest request, SimpMessageHeaderAccessor headerAccessor) {
        SessionInfo sessionInfo = sessionService.requireSession(resolveSessionToken(headerAccessor));
        int score = request == null || request.score() == null ? 0 : request.score();
        String gameName = request == null ? null : request.gameName();
        chatService.sendAnnouncement(sessionInfo.nickname(), gameName, score);
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
