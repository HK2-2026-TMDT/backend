package vn.io.sanmaymac.modules.user.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.common.enums.Role;
import vn.io.sanmaymac.modules.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
	Optional<UserEntity> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByRole(Role role);

	List<UserEntity> findByRole(Role role);
}
