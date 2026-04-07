package likelion.simsim.presence.dto;

public record OnlineUserResponse(
        String nickname,
        String school,
        long connectedAt,
        long connectedSeconds
) {
}
