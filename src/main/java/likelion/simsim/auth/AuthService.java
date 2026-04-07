package likelion.simsim.auth;

import likelion.simsim.auth.dto.AuthMeResponse;
import likelion.simsim.auth.dto.AuthResponse;
import likelion.simsim.auth.dto.LoginRequest;
import likelion.simsim.auth.dto.SignupRequest;
import likelion.simsim.auth.entity.UserEntity;
import likelion.simsim.auth.model.SessionInfo;
import likelion.simsim.auth.model.UserAccount;
import likelion.simsim.auth.repository.UserRepository;
import likelion.simsim.common.RedisKeys;
import likelion.simsim.common.exception.ConflictException;
import likelion.simsim.common.exception.UnauthorizedException;
import likelion.simsim.presence.PresenceService;
import likelion.simsim.ranking.RankingService;
import likelion.simsim.ranking.dto.UserRankingSummaryResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 회원가입, 로그인, 내 정보 조회, 로그아웃을 담당합니다.
 */
@Service
public class AuthService {

    private static final String FIELD_PASSWORD_HASH = "passwordHash";
    private static final String FIELD_SCHOOL = "school";
    private static final String FIELD_CREATED_AT = "createdAt";

    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final PresenceService presenceService;
    private final RankingService rankingService;

    public AuthService(
            UserRepository userRepository,
            StringRedisTemplate stringRedisTemplate,
            PasswordEncoder passwordEncoder,
            SessionService sessionService,
            PresenceService presenceService,
            RankingService rankingService
    ) {
        this.userRepository = userRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.presenceService = presenceService;
        this.rankingService = rankingService;
    }

    public void signup(SignupRequest request) {
        String nickname = normalize(request.nickname());
        String school = normalize(request.school());
        String password = normalize(request.password());

        if (findUser(nickname).isPresent()) {
            throw new ConflictException("이미 사용 중인 닉네임입니다.");
        }

        UserEntity userEntity = new UserEntity(
                nickname,
                passwordEncoder.encode(password),
                school,
                System.currentTimeMillis()
        );

        userRepository.save(userEntity);

        rankingService.initializeUserRecord(userEntity.getNickname(), userEntity.getSchool());
    }

    public AuthResponse login(LoginRequest request) {
        String nickname = normalize(request.nickname());
        String password = normalize(request.password());

        UserAccount userAccount = findUser(nickname)
                .orElseThrow(() -> new UnauthorizedException("닉네임 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, userAccount.passwordHash())) {
            throw new UnauthorizedException("닉네임 또는 비밀번호가 올바르지 않습니다.");
        }

        if (sessionService.hasActiveSessionByNickname(nickname)) {
            throw new ConflictException("이미 접속 중인 닉네임입니다. 초보자용 단순 정책으로 중복 로그인을 거절합니다.");
        }

        rankingService.initializeUserRecord(userAccount.nickname(), userAccount.school());

        String sessionToken = sessionService.createSession(userAccount.nickname(), userAccount.school());
        return new AuthResponse(sessionToken, userAccount.nickname(), userAccount.school());
    }

    public AuthMeResponse me(String sessionToken) {
        SessionInfo sessionInfo = sessionService.requireSession(sessionToken);
        UserRankingSummaryResponse rankingSummary = rankingService.getUserSummary(sessionInfo.nickname());
        return new AuthMeResponse(
                sessionInfo.sessionToken(),
                sessionInfo.nickname(),
                sessionInfo.school(),
                sessionInfo.isConnected(),
                sessionInfo.connectedAt(),
                sessionInfo.lastHeartbeat(),
                rankingSummary.totalConnectedSeconds(),
                rankingSummary.rank(),
                rankingSummary.updatedAt()
        );
    }

    public void logout(String sessionToken) {
        presenceService.cleanupSession(sessionToken, "로그아웃", true);
    }

    public Optional<UserAccount> findUser(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return Optional.empty();
        }

        Optional<UserAccount> mysqlUser = userRepository.findById(nickname)
                .map(this::toUserAccount);
        if (mysqlUser.isPresent()) {
            return mysqlUser;
        }

        Optional<UserAccount> legacyUser = findLegacyRedisUser(nickname);
        legacyUser.ifPresent(this::migrateLegacyUserToMysql);
        return legacyUser;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private Optional<UserAccount> findLegacyRedisUser(String nickname) {
        var entries = stringRedisTemplate.opsForHash().entries(RedisKeys.userKey(nickname));
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new UserAccount(
                nickname,
                String.valueOf(entries.get(FIELD_PASSWORD_HASH)),
                String.valueOf(entries.get(FIELD_SCHOOL)),
                Long.parseLong(String.valueOf(entries.get(FIELD_CREATED_AT)))
        ));
    }

    private void migrateLegacyUserToMysql(UserAccount userAccount) {
        if (userRepository.existsById(userAccount.nickname())) {
            return;
        }

        userRepository.save(new UserEntity(
                userAccount.nickname(),
                userAccount.passwordHash(),
                userAccount.school(),
                userAccount.createdAt()
        ));
    }

    private UserAccount toUserAccount(UserEntity entity) {
        return new UserAccount(
                entity.getNickname(),
                entity.getPasswordHash(),
                entity.getSchool(),
                entity.getCreatedAt()
        );
    }
}
