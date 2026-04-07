package likelion.simsim.ranking;

import likelion.simsim.auth.SessionService;
import likelion.simsim.auth.model.SessionInfo;
import likelion.simsim.common.RedisKeys;
import likelion.simsim.ranking.dto.RankingEntryResponse;
import likelion.simsim.ranking.dto.RankingResponse;
import likelion.simsim.ranking.dto.UserRankingSummaryResponse;
import likelion.simsim.ranking.entity.UserRankingStatEntity;
import likelion.simsim.ranking.repository.UserRankingStatRepository;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 전체 누적 접속 시간을 MySQL에 저장하고,
 * 현재 접속 중인 세션 시간은 조회 시점에 합산합니다.
 */
@Service
public class RankingService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SessionService sessionService;
    private final UserRankingStatRepository userRankingStatRepository;

    public RankingService(
            StringRedisTemplate stringRedisTemplate,
            SessionService sessionService,
            UserRankingStatRepository userRankingStatRepository
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sessionService = sessionService;
        this.userRankingStatRepository = userRankingStatRepository;
    }

    @Transactional
    public void initializeUserRecord(String nickname, String school) {
        long now = System.currentTimeMillis();

        userRankingStatRepository.findById(nickname)
                .ifPresentOrElse(entity -> {
                    if (!Objects.equals(entity.getSchool(), school)) {
                        entity.setSchool(school);
                    }
                    entity.touch(now);
                    userRankingStatRepository.save(entity);
                }, () -> userRankingStatRepository.save(
                        new UserRankingStatEntity(nickname, school, 0L, now, now)
                ));
    }

    @Transactional
    public void recordDisconnectedSession(SessionInfo sessionInfo) {
        if (sessionInfo == null || !sessionInfo.isConnected()) {
            return;
        }

        long additionalSeconds = Math.max(0L, (System.currentTimeMillis() - sessionInfo.connectedAt()) / 1000L);
        long now = System.currentTimeMillis();

        UserRankingStatEntity entity = userRankingStatRepository.findById(sessionInfo.nickname())
                .orElseGet(() -> new UserRankingStatEntity(
                        sessionInfo.nickname(),
                        sessionInfo.school(),
                        0L,
                        now,
                        now
                ));

        entity.setSchool(sessionInfo.school());
        entity.addConnectedSeconds(additionalSeconds, now);
        userRankingStatRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public RankingResponse getTop10() {
        long now = System.currentTimeMillis();
        List<RankingCandidate> candidates = buildCandidates(now);

        List<RankingEntryResponse> top10 = new ArrayList<>();
        int limit = Math.min(candidates.size(), 10);
        for (int index = 0; index < limit; index++) {
            RankingCandidate candidate = candidates.get(index);
            top10.add(new RankingEntryResponse(
                    index + 1,
                    candidate.nickname(),
                    candidate.school(),
                    candidate.totalConnectedSeconds(),
                    candidate.online()
            ));
        }

        return new RankingResponse(top10, now);
    }

    @Transactional(readOnly = true)
    public UserRankingSummaryResponse getUserSummary(String nickname) {
        long now = System.currentTimeMillis();
        List<RankingCandidate> candidates = buildCandidates(now);

        for (int index = 0; index < candidates.size(); index++) {
            RankingCandidate candidate = candidates.get(index);
            if (candidate.nickname().equals(nickname)) {
                return new UserRankingSummaryResponse(
                        candidate.totalConnectedSeconds(),
                        index + 1,
                        candidate.online(),
                        now
                );
            }
        }

        return new UserRankingSummaryResponse(0L, 0, false, now);
    }

    private List<RankingCandidate> buildCandidates(long now) {
        Map<String, LiveSessionSnapshot> activeSessions = loadActiveSessions(now);

        List<UserRankingStatEntity> storedStats = new ArrayList<>(
                userRankingStatRepository.findAllByOrderByTotalConnectedSecondsDescUpdatedAtAscNicknameAsc()
        );

        Set<String> knownNicknames = new HashSet<>();
        for (UserRankingStatEntity storedStat : storedStats) {
            knownNicknames.add(storedStat.getNickname());
        }

        for (LiveSessionSnapshot activeSession : activeSessions.values()) {
            if (!knownNicknames.contains(activeSession.nickname())) {
                storedStats.add(new UserRankingStatEntity(
                        activeSession.nickname(),
                        activeSession.school(),
                        0L,
                        now,
                        now
                ));
            }
        }

        List<RankingCandidate> candidates = new ArrayList<>();
        for (UserRankingStatEntity storedStat : storedStats) {
            LiveSessionSnapshot activeSession = activeSessions.get(storedStat.getNickname());
            long liveSeconds = activeSession == null ? 0L : activeSession.liveSeconds(now);

            candidates.add(new RankingCandidate(
                    storedStat.getNickname(),
                    activeSession == null ? storedStat.getSchool() : activeSession.school(),
                    storedStat.getTotalConnectedSeconds() + liveSeconds,
                    activeSession != null
            ));
        }

        candidates.sort(Comparator
                .comparingLong(RankingCandidate::totalConnectedSeconds).reversed()
                .thenComparing(RankingCandidate::nickname));

        return candidates;
    }

    private Map<String, LiveSessionSnapshot> loadActiveSessions(long now) {
        Map<String, LiveSessionSnapshot> activeSessions = new HashMap<>();

        try {
            Set<String> tokens = stringRedisTemplate.opsForZSet().range(RedisKeys.PRESENCE_KEY, 0, -1);
            if (tokens == null || tokens.isEmpty()) {
                return activeSessions;
            }

            for (String token : tokens) {
                SessionInfo sessionInfo = sessionService.findSession(token).orElse(null);
                if (sessionInfo == null) {
                    stringRedisTemplate.opsForZSet().remove(RedisKeys.PRESENCE_KEY, token);
                    continue;
                }

                if (!sessionInfo.isConnected()) {
                    continue;
                }

                activeSessions.put(sessionInfo.nickname(), new LiveSessionSnapshot(
                        sessionInfo.nickname(),
                        sessionInfo.school(),
                        sessionInfo.connectedAt(),
                        Math.max(0L, (now - sessionInfo.connectedAt()) / 1000L)
                ));
            }
        } catch (RedisConnectionFailureException ignored) {
            return activeSessions;
        }

        return activeSessions;
    }

    private record LiveSessionSnapshot(
            String nickname,
            String school,
            long connectedAt,
            long liveSeconds
    ) {
        private long liveSeconds(long now) {
            if (liveSeconds > 0L) {
                return liveSeconds;
            }
            return Math.max(0L, (now - connectedAt) / 1000L);
        }
    }

    private record RankingCandidate(
            String nickname,
            String school,
            long totalConnectedSeconds,
            boolean online
    ) {
    }
}
