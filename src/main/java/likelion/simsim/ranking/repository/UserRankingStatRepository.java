package likelion.simsim.ranking.repository;

import likelion.simsim.ranking.entity.UserRankingStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRankingStatRepository extends JpaRepository<UserRankingStatEntity, String> {

    List<UserRankingStatEntity> findAllByOrderByTotalConnectedSecondsDescUpdatedAtAscNicknameAsc();
}
