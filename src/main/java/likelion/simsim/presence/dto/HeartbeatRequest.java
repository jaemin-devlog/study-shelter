package likelion.simsim.presence.dto;

/**
 * 현재 서버는 payload 내용을 사용하지 않지만,
 * heartbeat 메시지 형태를 명확히 하기 위해 DTO 를 둡니다.
 */
public record HeartbeatRequest(
        Long clientTime
) {
}
