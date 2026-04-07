package likelion.simsim.presence.dto;

public record PresenceNoticeResponse(
        String nickname,
        long onlineCount,
        long createdAt
) {
}
