package likelion.simsim.feedback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "feedback_posts")
public class FeedbackPostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, length = 40)
    private String school;

    @Column(nullable = false, length = 80)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private long createdAt;

    protected FeedbackPostEntity() {
    }

    public FeedbackPostEntity(String nickname, String school, String title, String content, long createdAt) {
        this.nickname = nickname;
        this.school = school;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getSchool() {
        return school;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
