package likelion.simsim.presence;

import likelion.simsim.auth.SessionService;
import likelion.simsim.auth.model.SessionInfo;
import likelion.simsim.common.RedisKeys;
import likelion.simsim.config.DdanjitProperties;
import likelion.simsim.presence.dto.OnlineCountResponse;
import likelion.simsim.presence.dto.OnlineUserResponse;
import likelion.simsim.presence.dto.OnlineUsersResponse;
import likelion.simsim.presence.dto.PresenceNoticeResponse;
import likelion.simsim.ranking.RankingService;
import likelion.simsim.ranking.dto.RankingResponse;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 온라인 인원, heartbeat, stale cleanup, 랭킹 브로드캐스트를 담당합니다.
 */
@Service
public class PresenceService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SessionService sessionService;
    private final RankingService rankingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DdanjitProperties properties;

    public PresenceService(
            StringRedisTemplate stringRedisTemplate,
            SessionService sessionService,
            RankingService rankingService,
            SimpMessagingTemplate messagingTemplate,
            DdanjitProperties properties
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sessionService = sessionService;
        this.rankingService = rankingService;
        this.messagingTemplate = messagingTemplate;
        this.properties = properties;
    }

    public void handleHeartbeat(String sessionToken) {
        sessionService.refreshHeartbeat(sessionToken);
        SessionInfo sessionInfo = sessionService.requireSession(sessionToken);
        stringRedisTemplate.opsForZSet().add(RedisKeys.PRESENCE_KEY, sessionToken, sessionInfo.lastHeartbeat());
    }

    public void announceConnectedSession(String sessionToken) {
        SessionInfo sessionInfo = sessionService.requireSession(sessionToken);
        broadcastSnapshots();
        broadcastPresenceNotice(sessionInfo.nickname());
    }

    public void unregisterByWebSocketSessionId(String stompSessionId) {
        sessionService.findTokenByWebSocketSessionId(stompSessionId)
                .ifPresent(token -> cleanupSession(token, "연결 종료", true));
    }

    public void cleanupSession(String sessionToken, String reason, boolean broadcastSystemMessage) {
        CleanupOutcome outcome = cleanupSessionInternal(sessionToken);
        if (!outcome.shouldBroadcast()) {
            return;
        }

        broadcastSnapshots();
    }

    public void cleanupStaleSessions() {
        long threshold = System.currentTimeMillis() - properties.getStaleThresholdMillis();
        Set<String> staleTokens = stringRedisTemplate.opsForZSet()
                .rangeByScore(RedisKeys.PRESENCE_KEY, 0, threshold);

        if (staleTokens == null || staleTokens.isEmpty()) {
            return;
        }

        boolean changed = false;
        List<SessionInfo> removedSessions = new ArrayList<>();

        for (String token : staleTokens) {
            CleanupOutcome outcome = cleanupSessionInternal(token);
            if (outcome.shouldBroadcast()) {
                changed = true;
            }
            if (outcome.sessionInfo() != null && outcome.sessionInfo().isConnected()) {
                removedSessions.add(outcome.sessionInfo());
            }
        }

        if (!changed) {
            return;
        }

        broadcastSnapshots();
    }

    public OnlineCountResponse getOnlineCount() {
        Long count = stringRedisTemplate.opsForZSet().zCard(RedisKeys.PRESENCE_KEY);
        return new OnlineCountResponse(count == null ? 0L : count, System.currentTimeMillis());
    }

    public OnlineUsersResponse getOnlineUsers() {
        long now = System.currentTimeMillis();
        List<OnlineUserResponse> users = new ArrayList<>();

        try {
            Set<String> tokens = stringRedisTemplate.opsForZSet().range(RedisKeys.PRESENCE_KEY, 0, -1);
            if (tokens == null || tokens.isEmpty()) {
                return new OnlineUsersResponse(users, now);
            }

            for (String token : tokens) {
                SessionInfo sessionInfo = sessionService.findSession(token).orElse(null);
                if (sessionInfo == null) {
                    stringRedisTemplate.opsForZSet().remove(RedisKeys.PRESENCE_KEY, token);
                    continue;
                }

                if (!sessionInfo.isConnected()) {
                    continue;
                }

                users.add(new OnlineUserResponse(
                        sessionInfo.nickname(),
                        sessionInfo.school(),
                        sessionInfo.connectedAt(),
                        Math.max(0L, (now - sessionInfo.connectedAt()) / 1000L)
                ));
            }
        } catch (RedisConnectionFailureException ignored) {
            return new OnlineUsersResponse(users, now);
        }

        users.sort(Comparator
                .comparingLong(OnlineUserResponse::connectedAt)
                .thenComparing(OnlineUserResponse::nickname));

        return new OnlineUsersResponse(users, now);
    }

    public long getHeartbeatIntervalMillis() {
        return properties.getHeartbeatIntervalMillis();
    }

    public void broadcastRankingSnapshot() {
        RankingResponse rankingResponse = rankingService.getTop10();
        messagingTemplate.convertAndSend("/topic/ranking", rankingResponse);
    }

    private void broadcastSnapshots() {
        messagingTemplate.convertAndSend("/topic/online-count", getOnlineCount());
        broadcastRankingSnapshot();
    }

    private void broadcastPresenceNotice(String nickname) {
        OnlineCountResponse countResponse = getOnlineCount();
        messagingTemplate.convertAndSend("/topic/presence-notice", new PresenceNoticeResponse(
                nickname,
                countResponse.count(),
                System.currentTimeMillis()
        ));
    }

    private CleanupOutcome cleanupSessionInternal(String sessionToken) {
        Optional<SessionInfo> sessionInfoOptional = sessionService.findSession(sessionToken);
        SessionInfo sessionInfo = sessionInfoOptional.orElse(null);

        Long presenceRemoved = stringRedisTemplate.opsForZSet().remove(RedisKeys.PRESENCE_KEY, sessionToken);

        boolean hadSession = sessionInfo != null;
        boolean wasOnline = (presenceRemoved != null && presenceRemoved > 0)
                || (sessionInfo != null && sessionInfo.isConnected());

        if (sessionInfo != null && sessionInfo.isConnected()) {
            rankingService.recordDisconnectedSession(sessionInfo);
        }

        if (hadSession) {
            sessionService.deleteSession(sessionToken);
        }

        return new CleanupOutcome(wasOnline, sessionInfo);
    }
    private record CleanupOutcome(
            boolean shouldBroadcast,
            SessionInfo sessionInfo
    ) {
    }
}
