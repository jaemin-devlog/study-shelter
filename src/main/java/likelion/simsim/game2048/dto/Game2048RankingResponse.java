package likelion.simsim.game2048.dto;

import java.util.List;

public record Game2048RankingResponse(
        List<Game2048RankingEntryResponse> entries,
        long myBestScore,
        int myRank,
        long updatedAt
) {
}
