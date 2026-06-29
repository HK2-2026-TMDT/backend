package vn.io.sanmaymac.modules.order.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.order.entity.OrderDetailEntity;

public interface OrderDetailRepository extends JpaRepository<OrderDetailEntity, Long> {
    List<OrderDetailEntity> findByOrderId(Long orderId);
}
