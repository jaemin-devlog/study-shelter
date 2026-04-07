package likelion.simsim.auth.dto;

public record AuthResponse(
        String sessionToken,
        String nickname,
        String school
) {
}
