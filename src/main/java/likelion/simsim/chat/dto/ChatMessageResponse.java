package likelion.simsim.chat.dto;

/**
 * 현재는 일반 채팅 메시지만 사용합니다.
 */
public record ChatMessageResponse(
        String type,
        String nickname,
        String school,
        String content,
        long sentAt
) {

    public static ChatMessageResponse chat(String nickname, String school, String content) {
        return new ChatMessageResponse("CHAT", nickname, school, content, System.currentTimeMillis());
    }
}
