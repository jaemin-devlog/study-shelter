package likelion.simsim.auth.model;

/**
 * Redis HASH 에 저장되는 사용자 계정 정보를 표현합니다.
 */
public record UserAccount(
        String nickname,
        String passwordHash,
        String school,
        long createdAt
) {
}
