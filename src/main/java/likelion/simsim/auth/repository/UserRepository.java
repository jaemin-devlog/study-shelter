package likelion.simsim.auth.repository;

import likelion.simsim.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
}
