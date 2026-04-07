package likelion.simsim.config;

import likelion.simsim.presence.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * 실제 연결 종료 이벤트를 받아 presence 상태를 정리합니다.
 */
@Component
public class WebSocketEventListener {

    private final PresenceService presenceService;

    public WebSocketEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        presenceService.unregisterByWebSocketSessionId(event.getSessionId());
    }
}
