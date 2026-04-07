package likelion.simsim.ranking.dto;

public record UserRankingSummaryResponse(
        long totalConnectedSeconds,
        int rank,
        boolean online,
        long updatedAt
) {
}
