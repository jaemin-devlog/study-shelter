package likelion.simsim.common;

/**
 * 인증 관련 문자열을 한곳에 모아두면 오타를 줄일 수 있습니다.
 */
public final class AuthConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String SESSION_TOKEN_HEADER = "X-Session-Token";
    public static final String STOMP_SESSION_TOKEN_HEADER = "sessionToken";

    public static final String SESSION_TOKEN_ATTR = "sessionToken";
    public static final String NICKNAME_ATTR = "nickname";
    public static final String SCHOOL_ATTR = "school";

    private AuthConstants() {
    }
}
