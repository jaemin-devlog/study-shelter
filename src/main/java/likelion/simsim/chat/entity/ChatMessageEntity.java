package likelion.simsim.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * 채팅 이력을 MySQL에 저장합니다.
 */
@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_messages_type_sent_at", columnList = "type, sentAt"),
                @Index(name = "idx_chat_messages_sent_at", columnList = "sentAt")
        }
)
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 100)
    private String school;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private long sentAt;

    protected ChatMessageEntity() {
    }

    public ChatMessageEntity(String type, String nickname, String school, String content, long sentAt) {
        this.type = type;
        this.nickname = nickname;
        this.school = school;
        this.content = content;
        this.sentAt = sentAt;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getNickname() {
        return nickname;
    }

    public String getSchool() {
        return school;
    }

    public String getContent() {
        return content;
    }

    public long getSentAt() {
        return sentAt;
    }
}
