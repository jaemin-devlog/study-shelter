package likelion.simsim.ranking.dto;

import java.util.List;

public record RankingResponse(
        List<RankingEntryResponse> rankings,
        long updatedAt
) {
}
