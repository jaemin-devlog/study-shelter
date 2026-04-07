package likelion.simsim.presence;

import likelion.simsim.auth.SessionService;
import likelion.simsim.auth.model.SessionInfo;
import likelion.simsim.common.RedisKeys;
import likelion.simsim.ranking.RankingService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 온라인 세션을 Redis presence/ranking 에 등록하는 전용 서비스입니다.
 * 메시지 브로드캐스트와 분리해서 순환 참조를 피합니다.
 */
@Service
public class PresenceRegistryService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SessionService sessionService;
    private final RankingService rankingService;

    public PresenceRegistryService(
            StringRedisTemplate stringRedisTemplate,
            SessionService sessionService,
            RankingService rankingService
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sessionService = sessionService;
        this.rankingService = rankingService;
    }

    public SessionInfo registerConnectedSession(String sessionToken, String stompSessionId) {
        sessionService.markConnected(sessionToken, stompSessionId);
        SessionInfo sessionInfo = sessionService.requireSession(sessionToken);

        stringRedisTemplate.opsForZSet().add(RedisKeys.PRESENCE_KEY, sessionToken, sessionInfo.lastHeartbeat());
        rankingService.initializeUserRecord(sessionInfo.nickname(), sessionInfo.school());

        return sessionInfo;
    }
}
