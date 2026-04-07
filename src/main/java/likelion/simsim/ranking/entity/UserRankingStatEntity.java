package likelion.simsim.ranking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 사용자별 누적 접속 시간을 MySQL에 영구 저장합니다.
 */
@Entity
@Table(name = "user_ranking_stats")
public class UserRankingStatEntity {

    @Id
    @Column(length = 50, nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false, length = 100)
    private String school;

    @Column(nullable = false)
    private long totalConnectedSeconds;

    @Column(nullable = false)
    private long createdAt;

    @Column(nullable = false)
    private long updatedAt;

    protected UserRankingStatEntity() {
    }

    public UserRankingStatEntity(String nickname, String school, long totalConnectedSeconds, long createdAt, long updatedAt) {
        this.nickname = nickname;
        this.school = school;
        this.totalConnectedSeconds = totalConnectedSeconds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getNickname() {
        return nickname;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public long getTotalConnectedSeconds() {
        return totalConnectedSeconds;
    }

    public void addConnectedSeconds(long additionalSeconds, long updatedAt) {
        this.totalConnectedSeconds += additionalSeconds;
        this.updatedAt = updatedAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void touch(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
