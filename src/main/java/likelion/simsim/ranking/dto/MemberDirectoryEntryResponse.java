package likelion.simsim.ranking.dto;

public record MemberDirectoryEntryResponse(
        int rank,
        String nickname,
        String school,
        long totalConnectedSeconds,
        boolean online
) {
}
