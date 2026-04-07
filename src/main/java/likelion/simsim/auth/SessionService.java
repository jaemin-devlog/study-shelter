package likelion.simsim.auth;

import likelion.simsim.auth.model.SessionInfo;
import likelion.simsim.common.RedisKeys;
import likelion.simsim.common.exception.UnauthorizedException;
import likelion.simsim.config.DdanjitProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 로그인 토큰과 WebSocket 세션 매핑을 Redis 에 저장합니다.
 */
@Service
public class SessionService {

    private static final String FIELD_NICKNAME = "nickname";
    private static final String FIELD_SCHOOL = "school";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_CONNECTED_AT = "connectedAt";
    private static final String FIELD_LAST_HEARTBEAT = "lastHeartbeat";
    private static final String FIELD_STOMP_SESSION_ID = "stompSessionId";

    private final StringRedisTemplate stringRedisTemplate;
    private final DdanjitProperties properties;

    public SessionService(StringRedisTemplate stringRedisTemplate, DdanjitProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    public String createSession(String nickname, String school) {
        long now = System.currentTimeMillis();
        String token = UUID.randomUUID().toString();

        String sessionKey = RedisKeys.sessionKey(token);
        Map<String, String> sessionFields = Map.of(
                FIELD_NICKNAME, nickname,
                FIELD_SCHOOL, school,
                FIELD_CREATED_AT, String.valueOf(now),
                FIELD_CONNECTED_AT, "0",
                FIELD_LAST_HEARTBEAT, "0",
                FIELD_STOMP_SESSION_ID, ""
        );

        stringRedisTemplate.opsForHash().putAll(sessionKey, sessionFields);
        expireSessionRelatedKeys(token, nickname, null);
        stringRedisTemplate.opsForValue().set(
                RedisKeys.nicknameSessionKey(nickname),
                token,
                Duration.ofSeconds(properties.getSessionTtlSeconds())
        );

        return token;
    }

    public Optional<SessionInfo> findSession(String sessionToken) {
        if (!StringUtils.hasText(sessionToken)) {
            return Optional.empty();
        }

        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(RedisKeys.sessionKey(sessionToken));
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        String nickname = asString(entries.get(FIELD_NICKNAME));
        String school = asString(entries.get(FIELD_SCHOOL));
        long createdAt = asLong(entries.get(FIELD_CREATED_AT));
        long connectedAt = asLong(entries.get(FIELD_CONNECTED_AT));
        long lastHeartbeat = asLong(entries.get(FIELD_LAST_HEARTBEAT));
        String stompSessionId = asString(entries.get(FIELD_STOMP_SESSION_ID));

        return Optional.of(new SessionInfo(
                sessionToken,
                nickname,
                school,
                createdAt,
                connectedAt,
                lastHeartbeat,
                stompSessionId
        ));
    }

    public SessionInfo requireSession(String sessionToken) {
        return findSession(sessionToken)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 세션 토큰입니다."));
    }

    public boolean hasActiveSessionByNickname(String nickname) {
        String mappedToken = stringRedisTemplate.opsForValue().get(RedisKeys.nicknameSessionKey(nickname));
        if (!StringUtils.hasText(mappedToken)) {
            return false;
        }

        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeys.sessionKey(mappedToken)))) {
            return true;
        }

        stringRedisTemplate.delete(RedisKeys.nicknameSessionKey(nickname));
        return false;
    }

    public boolean isTokenAlreadyConnected(String sessionToken) {
        return findSession(sessionToken)
                .map(SessionInfo::isConnected)
                .orElse(false);
    }

    public void markConnected(String sessionToken, String stompSessionId) {
        SessionInfo sessionInfo = requireSession(sessionToken);
        long now = System.currentTimeMillis();

        String sessionKey = RedisKeys.sessionKey(sessionToken);
        stringRedisTemplate.opsForHash().put(sessionKey, FIELD_CONNECTED_AT, String.valueOf(now));
        stringRedisTemplate.opsForHash().put(sessionKey, FIELD_LAST_HEARTBEAT, String.valueOf(now));
        stringRedisTemplate.opsForHash().put(sessionKey, FIELD_STOMP_SESSION_ID, stompSessionId);
        stringRedisTemplate.opsForValue().set(
                RedisKeys.webSocketSessionKey(stompSessionId),
                sessionToken,
                Duration.ofSeconds(properties.getSessionTtlSeconds())
        );

        expireSessionRelatedKeys(sessionToken, sessionInfo.nickname(), stompSessionId);
    }

    public void refreshHeartbeat(String sessionToken) {
        SessionInfo sessionInfo = requireSession(sessionToken);
        long now = System.currentTimeMillis();

        String sessionKey = RedisKeys.sessionKey(sessionToken);
        stringRedisTemplate.opsForHash().put(sessionKey, FIELD_LAST_HEARTBEAT, String.valueOf(now));
        expireSessionRelatedKeys(sessionToken, sessionInfo.nickname(), sessionInfo.stompSessionId());
    }

    public Optional<String> findTokenByWebSocketSessionId(String stompSessionId) {
        if (!StringUtils.hasText(stompSessionId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(RedisKeys.webSocketSessionKey(stompSessionId)));
    }

    public void deleteSession(String sessionToken) {
        findSession(sessionToken).ifPresent(sessionInfo -> {
            stringRedisTemplate.delete(RedisKeys.sessionKey(sessionToken));

            String nicknameKey = RedisKeys.nicknameSessionKey(sessionInfo.nickname());
            String mappedToken = stringRedisTemplate.opsForValue().get(nicknameKey);
            if (sessionToken.equals(mappedToken)) {
                stringRedisTemplate.delete(nicknameKey);
            }

            if (StringUtils.hasText(sessionInfo.stompSessionId())) {
                stringRedisTemplate.delete(RedisKeys.webSocketSessionKey(sessionInfo.stompSessionId()));
            }
        });
    }

    private void expireSessionRelatedKeys(String sessionToken, String nickname, String stompSessionId) {
        Duration ttl = Duration.ofSeconds(properties.getSessionTtlSeconds());
        stringRedisTemplate.expire(RedisKeys.sessionKey(sessionToken), ttl);
        stringRedisTemplate.expire(RedisKeys.nicknameSessionKey(nickname), ttl);

        if (StringUtils.hasText(stompSessionId)) {
            stringRedisTemplate.expire(RedisKeys.webSocketSessionKey(stompSessionId), ttl);
        }
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }
}
