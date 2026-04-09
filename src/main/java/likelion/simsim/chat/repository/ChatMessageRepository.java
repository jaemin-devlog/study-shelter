package likelion.simsim.chat.repository;

import likelion.simsim.chat.entity.ChatMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findAllByTypeOrderBySentAtDesc(String type, Pageable pageable);
}
