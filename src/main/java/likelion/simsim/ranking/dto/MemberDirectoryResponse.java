package likelion.simsim.ranking.dto;

import java.util.List;

public record MemberDirectoryResponse(
        List<MemberDirectoryEntryResponse> members,
        int page,
        int size,
        int totalPages,
        long totalElements,
        long updatedAt
) {
}
