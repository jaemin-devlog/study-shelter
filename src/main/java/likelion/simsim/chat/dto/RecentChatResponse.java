package likelion.simsim.chat.dto;

import java.util.List;

public record RecentChatResponse(
        List<ChatMessageResponse> messages
) {
}
