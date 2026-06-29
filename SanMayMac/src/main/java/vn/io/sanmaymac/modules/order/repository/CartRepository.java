package vn.io.sanmaymac.modules.order.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.order.entity.CartEntity;

public interface CartRepository extends JpaRepository<CartEntity, Long> {
    Optional<CartEntity> findByCustomerId(Long customerId);
}
