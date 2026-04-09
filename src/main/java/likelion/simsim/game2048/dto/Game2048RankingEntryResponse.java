package likelion.simsim.game2048.dto;

public record Game2048RankingEntryResponse(
        int rank,
        String nickname,
        String school,
        long bestScore
) {
}
