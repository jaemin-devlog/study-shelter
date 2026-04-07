package likelion.simsim.auth;

import likelion.simsim.common.AuthConstants;
import likelion.simsim.common.exception.UnauthorizedException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * REST 와 STOMP 에서 세션 토큰을 같은 방식으로 꺼내기 위한 도우미입니다.
 */
@Component
public class SessionTokenResolver {

    public String resolveOrThrow(String authorizationHeader, String sessionTokenHeader) {
        String token = resolve(authorizationHeader, sessionTokenHeader);
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("세션 토큰이 없습니다.");
        }
        return token;
    }

    public String resolve(String authorizationHeader, String sessionTokenHeader) {
        if (StringUtils.hasText(sessionTokenHeader)) {
            return sessionTokenHeader.trim();
        }

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();
        }

        return null;
    }

    public String resolveFromStompHeader(StompHeaderAccessor accessor) {
        List<String> values = accessor.getNativeHeader(AuthConstants.STOMP_SESSION_TOKEN_HEADER);
        if (values == null || values.isEmpty()) {
            return null;
        }
        String token = values.get(0);
        return StringUtils.hasText(token) ? token.trim() : null;
    }
}
