package likelion.simsim.auth.repository;

import likelion.simsim.auth.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, String> {
}
