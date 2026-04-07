package likelion.simsim.presence.dto;

import java.util.List;

public record OnlineUsersResponse(
        List<OnlineUserResponse> users,
        long updatedAt
) {
}
