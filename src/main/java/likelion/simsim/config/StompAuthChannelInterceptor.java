package likelion.simsim.config;

import likelion.simsim.auth.SessionService;
import likelion.simsim.auth.SessionTokenResolver;
import likelion.simsim.auth.model.SessionInfo;
import likelion.simsim.common.AuthConstants;
import likelion.simsim.common.exception.UnauthorizedException;
import likelion.simsim.presence.PresenceConnectedEvent;
import likelion.simsim.presence.PresenceRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * STOMP CONNECT, SEND 단계의 인증 흐름을 추적한다.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

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

        log.info(
                "STOMP_FRAME command={} sessionId={} destination={}",
                accessor.getCommand(),
                accessor.getSessionId(),
                accessor.getDestination()
        );

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
            log.warn("STOMP_CONNECT_REJECT reason=missing-session-token sessionId={}", accessor.getSessionId());
            throw new UnauthorizedException("WebSocket CONNECT 헤더에 sessionToken 이 필요합니다.");
        }

        SessionInfo sessionInfo = sessionService.requireSession(sessionToken);
        if (sessionService.isTokenAlreadyConnected(sessionToken)) {
            log.warn(
                    "STOMP_CONNECT_REJECT reason=already-connected nickname={} sessionId={}",
                    sessionInfo.nickname(),
                    accessor.getSessionId()
            );
            throw new UnauthorizedException("이미 연결된 세션입니다. 중복 WebSocket 접속은 허용하지 않습니다.");
        }

        accessor.setUser(new StompPrincipal(sessionInfo.nickname()));
        accessor.getSessionAttributes().put(AuthConstants.SESSION_TOKEN_ATTR, sessionToken);
        accessor.getSessionAttributes().put(AuthConstants.NICKNAME_ATTR, sessionInfo.nickname());
        accessor.getSessionAttributes().put(AuthConstants.SCHOOL_ATTR, sessionInfo.school());

        String stompSessionId = accessor.getSessionId();
        if (!StringUtils.hasText(stompSessionId)) {
            log.warn("STOMP_CONNECT_REJECT reason=missing-stomp-session-id nickname={}", sessionInfo.nickname());
            throw new UnauthorizedException("WebSocket 세션 ID를 확인할 수 없습니다.");
        }

        log.info(
                "STOMP_CONNECT_ACCEPT nickname={} sessionId={} tokenSuffix={}",
                sessionInfo.nickname(),
                stompSessionId,
                sessionToken.length() > 8 ? sessionToken.substring(sessionToken.length() - 8) : sessionToken
        );

        presenceRegistryService.registerConnectedSession(sessionToken, stompSessionId);
        applicationEventPublisher.publishEvent(new PresenceConnectedEvent(sessionToken));
    }

    private void validateSend(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.warn("STOMP_SEND_REJECT reason=missing-session-attributes sessionId={}", accessor.getSessionId());
            throw new UnauthorizedException("WebSocket 세션 정보가 없습니다.");
        }

        Object tokenValue = sessionAttributes.get(AuthConstants.SESSION_TOKEN_ATTR);
        String sessionToken = tokenValue == null ? null : String.valueOf(tokenValue);
        if (!StringUtils.hasText(sessionToken)) {
            log.warn("STOMP_SEND_REJECT reason=missing-session-token sessionId={}", accessor.getSessionId());
            throw new UnauthorizedException("세션 토큰이 없습니다.");
        }

        log.info("STOMP_SEND_ACCEPT sessionId={} destination={}", accessor.getSessionId(), accessor.getDestination());
        sessionService.requireSession(sessionToken);
    }
}
