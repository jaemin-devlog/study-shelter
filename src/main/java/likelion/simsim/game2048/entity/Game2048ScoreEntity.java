package likelion.simsim.game2048.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "game_2048_scores",
        indexes = {
                @Index(name = "idx_game_2048_scores_best_score", columnList = "bestScore, updatedAt")
        }
)
public class Game2048ScoreEntity {

    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(nullable = false, length = 100)
    private String school;

    @Column(nullable = false)
    private long bestScore;

    @Column(nullable = false)
    private long createdAt;

    @Column(nullable = false)
    private long updatedAt;

    protected Game2048ScoreEntity() {
    }

    public Game2048ScoreEntity(String nickname, String school, long bestScore, long createdAt, long updatedAt) {
        this.nickname = nickname;
        this.school = school;
        this.bestScore = bestScore;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getNickname() {
        return nickname;
    }

    public String getSchool() {
        return school;
    }

    public long getBestScore() {
        return bestScore;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void updateRecord(String school, long score, long updatedAt) {
        this.school = school;
        this.bestScore = Math.max(this.bestScore, score);
        this.updatedAt = updatedAt;
    }
}
