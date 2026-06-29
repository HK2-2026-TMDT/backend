package vn.io.sanmaymac.modules.user.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.user.entity.UserAddressEntity;

public interface UserAddressRepository extends JpaRepository<UserAddressEntity, Long> {
    List<UserAddressEntity> findByUserId(Long userId);

    Optional<UserAddressEntity> findByIdAndUserId(Long id, Long userId);
}
