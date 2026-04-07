package likelion.simsim.auth.dto;

public record AuthMeResponse(
        String sessionToken,
        String nickname,
        String school,
        boolean connected,
        long connectedAt,
        long lastHeartbeat,
        long totalConnectedSeconds,
        int rankingPosition,
        long totalUpdatedAt
) {
}
