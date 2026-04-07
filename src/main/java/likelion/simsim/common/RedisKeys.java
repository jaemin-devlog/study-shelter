package likelion.simsim.common;

/**
 * Redis 키 이름을 한 곳에서 관리합니다.
 */
public final class RedisKeys {

    public static final String PRESENCE_KEY = "ddanjit:presence";
    public static final String RANKING_KEY = "ddanjit:ranking";
    public static final String CHAT_RECENT_KEY = "ddanjit:chat:recent";

    private RedisKeys() {
    }

    public static String userKey(String nickname) {
        return "ddanjit:user:" + nickname;
    }

    public static String sessionKey(String sessionToken) {
        return "ddanjit:session:" + sessionToken;
    }

    public static String nicknameSessionKey(String nickname) {
        return "ddanjit:nickname-session:" + nickname;
    }

    public static String webSocketSessionKey(String stompSessionId) {
        return "ddanjit:ws-session:" + stompSessionId;
    }
}
