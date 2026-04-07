package likelion.simsim.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 회원 계정의 기준 저장소는 users 테이블이다.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(nullable = false, length = 12, unique = true)
    private String nickname;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String school;

    @Column(nullable = false)
    private long createdAt;

    protected UserEntity() {
    }

    public UserEntity(String nickname, String passwordHash, String school, long createdAt) {
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.school = school;
        this.createdAt = createdAt;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSchool() {
        return school;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
