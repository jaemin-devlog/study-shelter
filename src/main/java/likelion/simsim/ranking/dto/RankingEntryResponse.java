package likelion.simsim.ranking.dto;

public record RankingEntryResponse(
        int rank,
        String nickname,
        String school,
        long totalConnectedSeconds,
        boolean online
) {
}
