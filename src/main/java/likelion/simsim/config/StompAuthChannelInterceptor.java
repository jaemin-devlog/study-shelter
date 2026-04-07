package likelion.simsim.config;

import likelion.simsim.auth.SessionService;
import likelion.simsim.auth.SessionTokenResolver;
import likelion.simsim.auth.model.SessionInfo;
import likelion.simsim.common.AuthConstants;
import likelion.simsim.common.exception.UnauthorizedException;
import likelion.simsim.presence.PresenceConnectedEvent;
import likelion.simsim.presence.PresenceRegistryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * WebSocket CONNECT 시점에 sessionToken 을 검사하고
 * 이후 메시지 전송 시에도 세션이 살아있는지 확인합니다.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final SessionTokenResolver sessionTokenResolver;
    private final SessionService sessionService;
    private final PresenceRegistryService presenceRegistryService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public StompAuthChannelInterceptor(
            SessionTokenResolver sessionTokenResolver,
            SessionService sessionService,
            PresenceRegistryService presenceRegistryService,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.sessionTokenResolver = sessionTokenResolver;
        this.sessionService = sessionService;
        this.presenceRegistryService = presenceRegistryService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            validateSend(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String sessionToken = sessionTokenResolver.resolveFromStompHeader(accessor);
        if (!StringUtils.hasText(sessionToken)) {
            throw new UnauthorizedException("WebSocket CONNECT 헤더에 sessionToken 이 필요합니다.");
        }

        SessionInfo sessionInfo = sessionService.requireSession(sessionToken);
        if (sessionService.isTokenAlreadyConnected(sessionToken)) {
            throw new UnauthorizedException("이미 연결된 세션입니다. 중복 WebSocket 접속은 허용하지 않습니다.");
        }

        accessor.setUser(new StompPrincipal(sessionInfo.nickname()));
        accessor.getSessionAttributes().put(AuthConstants.SESSION_TOKEN_ATTR, sessionToken);
        accessor.getSessionAttributes().put(AuthConstants.NICKNAME_ATTR, sessionInfo.nickname());
        accessor.getSessionAttributes().put(AuthConstants.SCHOOL_ATTR, sessionInfo.school());

        String stompSessionId = accessor.getSessionId();
        if (!StringUtils.hasText(stompSessionId)) {
            throw new UnauthorizedException("WebSocket 세션 ID를 확인할 수 없습니다.");
        }

        // CONNECT 프레임 단계에서 즉시 온라인 세션으로 등록해야
        // 프론트가 연결 직후 조회하는 내 시간/랭킹에 바로 반영됩니다.
        presenceRegistryService.registerConnectedSession(sessionToken, stompSessionId);
        applicationEventPublisher.publishEvent(new PresenceConnectedEvent(sessionToken));
    }

    private void validateSend(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new UnauthorizedException("WebSocket 세션 정보가 없습니다.");
        }

        Object tokenValue = sessionAttributes.get(AuthConstants.SESSION_TOKEN_ATTR);
        String sessionToken = tokenValue == null ? null : String.valueOf(tokenValue);
        if (!StringUtils.hasText(sessionToken)) {
            throw new UnauthorizedException("세션 토큰이 없습니다.");
        }

        sessionService.requireSession(sessionToken);
    }
}
