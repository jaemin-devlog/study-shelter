package likelion.simsim.presence;

/**
 * CONNECT 인증이 끝난 직후 온라인 등록 완료를 알리는 이벤트입니다.
 */
public record PresenceConnectedEvent(
        String sessionToken
) {
}
