package likelion.simsim.feedback.repository;

import likelion.simsim.feedback.entity.FeedbackPostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackPostRepository extends JpaRepository<FeedbackPostEntity, Long> {

    List<FeedbackPostEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
