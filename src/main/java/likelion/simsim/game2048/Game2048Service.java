package likelion.simsim.game2048;

import likelion.simsim.auth.model.SessionInfo;
import likelion.simsim.chat.ChatService;
import likelion.simsim.common.exception.BadRequestException;
import likelion.simsim.game2048.dto.Game2048RankingEntryResponse;
import likelion.simsim.game2048.dto.Game2048RankingResponse;
import likelion.simsim.game2048.entity.Game2048ScoreEntity;
import likelion.simsim.game2048.repository.Game2048ScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class Game2048Service {

    private final Game2048ScoreRepository game2048ScoreRepository;
    private final ChatService chatService;

    public Game2048Service(
            Game2048ScoreRepository game2048ScoreRepository,
            ChatService chatService
    ) {
        this.game2048ScoreRepository = game2048ScoreRepository;
        this.chatService = chatService;
    }

    @Transactional(readOnly = true)
    public Game2048RankingResponse getRanking(String nickname) {
        return buildRankingResponse(nickname, System.currentTimeMillis());
    }

    @Transactional
    public Game2048RankingResponse saveScore(SessionInfo sessionInfo, long score) {
        validateScore(score);

        long now = System.currentTimeMillis();
        Game2048ScoreEntity entity = game2048ScoreRepository.findById(sessionInfo.nickname())
                .orElseGet(() -> new Game2048ScoreEntity(
                        sessionInfo.nickname(),
                        sessionInfo.school(),
                        0L,
                        now,
                        now
                ));

        entity.updateRecord(sessionInfo.school(), score, now);
        game2048ScoreRepository.save(entity);

        return buildRankingResponse(sessionInfo.nickname(), now);
    }

    @Transactional
    public Game2048RankingResponse shareScore(SessionInfo sessionInfo, long score) {
        Game2048RankingResponse response = saveScore(sessionInfo, score);
        chatService.sendAnnouncement(sessionInfo.nickname() + "님이 " + score + "점을 달성하였습니다!");
        return response;
    }

    private void validateScore(long score) {
        if (score <= 0) {
            throw new BadRequestException("공유할 점수는 1점 이상이어야 합니다.");
        }
    }

    private Game2048RankingResponse buildRankingResponse(String nickname, long updatedAt) {
        List<Game2048ScoreEntity> entities = game2048ScoreRepository.findAllByOrderByBestScoreDescUpdatedAtAscNicknameAsc();
        List<Game2048RankingEntryResponse> entries = new ArrayList<>();
        long myBestScore = 0L;
        int myRank = 0;

        for (int index = 0; index < entities.size(); index += 1) {
            Game2048ScoreEntity entity = entities.get(index);
            int rank = index + 1;

            if (rank <= 10) {
                entries.add(new Game2048RankingEntryResponse(
                        rank,
                        entity.getNickname(),
                        entity.getSchool(),
                        entity.getBestScore()
                ));
            }

            if (nickname != null && nickname.equals(entity.getNickname())) {
                myBestScore = entity.getBestScore();
                myRank = rank;
            }
        }

        return new Game2048RankingResponse(entries, myBestScore, myRank, updatedAt);
    }
}
