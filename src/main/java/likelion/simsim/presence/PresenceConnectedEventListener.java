package likelion.simsim.presence;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 온라인 등록 직후 시스템 메시지와 스냅샷 브로드캐스트를 수행합니다.
 */
@Component
public class PresenceConnectedEventListener {

    private final PresenceService presenceService;

    public PresenceConnectedEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handlePresenceConnected(PresenceConnectedEvent event) {
        presenceService.announceConnectedSession(event.sessionToken());
    }
}
