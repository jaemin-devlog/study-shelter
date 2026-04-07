package likelion.simsim.auth.model;

/**
 * 로그인 후 발급되는 세션 토큰의 Redis 저장 내용을 표현합니다.
 */
public record SessionInfo(
        String sessionToken,
        String nickname,
        String school,
        long createdAt,
        long connectedAt,
        long lastHeartbeat,
        String stompSessionId
) {

    public boolean isConnected() {
        return connectedAt > 0L;
    }
}
