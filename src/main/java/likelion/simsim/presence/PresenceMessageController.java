package likelion.simsim.presence;

import likelion.simsim.common.AuthConstants;
import likelion.simsim.common.exception.UnauthorizedException;
import likelion.simsim.presence.dto.HeartbeatRequest;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 클라이언트 heartbeat 메시지를 받아 lastHeartbeat 를 갱신합니다.
 */
@Controller
public class PresenceMessageController {

    private final PresenceService presenceService;

    public PresenceMessageController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @MessageMapping("/presence.heartbeat")
    public void heartbeat(HeartbeatRequest request, SimpMessageHeaderAccessor headerAccessor) {
        presenceService.handleHeartbeat(resolveSessionToken(headerAccessor));
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
