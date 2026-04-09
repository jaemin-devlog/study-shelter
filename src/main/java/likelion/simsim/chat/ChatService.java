package likelion.simsim.chat;

import likelion.simsim.chat.dto.ChatMessageResponse;
import likelion.simsim.chat.dto.RecentChatResponse;
import likelion.simsim.chat.entity.ChatMessageEntity;
import likelion.simsim.chat.repository.ChatMessageRepository;
import likelion.simsim.common.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatService {

    private static final String CHAT_TYPE = "CHAT";
    private static final String ANNOUNCEMENT_TYPE = "ANNOUNCEMENT";
    private static final int MAX_CHAT_LENGTH = 200;
    private static final int MAX_CHAT_HISTORY_SIZE = 100;

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(
            SimpMessagingTemplate messagingTemplate,
            ChatMessageRepository chatMessageRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
    }

    public void sendChat(String nickname, String school, String rawContent) {
        String normalizedContent = normalizeContent(rawContent);
        String escapedContent = HtmlUtils.htmlEscape(normalizedContent);
        ChatMessageResponse message = ChatMessageResponse.chat(nickname, school, escapedContent);
        saveRecentMessage(message);
        messagingTemplate.convertAndSend("/topic/chat", message);
    }

    public void sendAnnouncement(String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isBlank()) {
            throw new BadRequestException("공유할 점수 문구가 비어 있을 수 없습니다.");
        }

        ChatMessageResponse message = ChatMessageResponse.announcement(HtmlUtils.htmlEscape(content));
        saveRecentMessage(message);
        messagingTemplate.convertAndSend("/topic/chat", message);
    }

    public RecentChatResponse getRecentMessages() {
        List<ChatMessageEntity> entities = chatMessageRepository.findAllByTypeInOrderBySentAtDesc(
                List.of(CHAT_TYPE, ANNOUNCEMENT_TYPE),
                PageRequest.of(0, MAX_CHAT_HISTORY_SIZE)
        );
        List<ChatMessageResponse> messages = new ArrayList<>();

        if (entities.isEmpty()) {
            return new RecentChatResponse(messages);
        }

        for (ChatMessageEntity entity : entities) {
            messages.add(new ChatMessageResponse(
                    entity.getType(),
                    entity.getNickname(),
                    entity.getSchool(),
                    entity.getContent(),
                    entity.getSentAt()
            ));
        }

        Collections.reverse(messages);
        return new RecentChatResponse(messages);
    }

    private String normalizeContent(String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isBlank()) {
            throw new BadRequestException("채팅 내용은 비어 있을 수 없습니다.");
        }
        if (content.length() > MAX_CHAT_LENGTH) {
            throw new BadRequestException("채팅은 200자 이하로 입력해 주세요.");
        }
        return content;
    }

    private void saveRecentMessage(ChatMessageResponse message) {
        chatMessageRepository.save(new ChatMessageEntity(
                message.type(),
                message.nickname(),
                message.school(),
                message.content(),
                message.sentAt()
        ));
    }
}
