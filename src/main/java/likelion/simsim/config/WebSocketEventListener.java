package likelion.simsim.config;

import likelion.simsim.presence.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 이벤트 흐름을 로그로 남긴다.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final PresenceService presenceService;

    public WebSocketEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        log.info("WS_EVENT type=SessionConnect");
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        log.info("WS_EVENT type=SessionConnected");
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        log.info("WS_EVENT type=SessionDisconnect sessionId={}", event.getSessionId());
        presenceService.unregisterByWebSocketSessionId(event.getSessionId());
    }
}
