package likelion.simsim.scheduler;

import likelion.simsim.presence.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * heartbeat 가 끊긴 세션을 주기적으로 정리합니다.
 */
@Component
public class PresenceCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(PresenceCleanupScheduler.class);

    private final PresenceService presenceService;
    private final AtomicBoolean redisUnavailableLogged = new AtomicBoolean(false);

    public PresenceCleanupScheduler(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Scheduled(fixedDelayString = "${ddanjit.cleanup-fixed-delay-millis:15000}")
    public void cleanupStalePresence() {
        try {
            presenceService.cleanupStaleSessions();

            if (redisUnavailableLogged.getAndSet(false)) {
                log.info("Redis 연결이 복구되어 stale presence cleanup 을 다시 수행합니다.");
            }
        } catch (RedisConnectionFailureException exception) {
            if (redisUnavailableLogged.compareAndSet(false, true)) {
                log.warn("Redis 에 연결할 수 없어 stale presence cleanup 을 건너뜁니다. Redis 가 올라오면 자동으로 재시도합니다.");
            }
        }
    }
}
