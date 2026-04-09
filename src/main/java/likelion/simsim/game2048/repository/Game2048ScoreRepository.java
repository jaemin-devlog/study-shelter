package likelion.simsim.game2048.repository;

import likelion.simsim.game2048.entity.Game2048ScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Game2048ScoreRepository extends JpaRepository<Game2048ScoreEntity, String> {

    List<Game2048ScoreEntity> findAllByOrderByBestScoreDescUpdatedAtAscNicknameAsc();
}
