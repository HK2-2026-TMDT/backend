package vn.io.sanmaymac.modules.order.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.common.enums.OrderStatus;
import vn.io.sanmaymac.common.enums.OrderType;
import vn.io.sanmaymac.modules.order.entity.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
	List<OrderEntity> findByCustomerId(Long customerId);

	List<OrderEntity> findByCustomerIdAndStatus(Long customerId, OrderStatus status);

	Page<OrderEntity> findByCustomerId(Long customerId, Pageable pageable);

	Page<OrderEntity> findByCustomerIdAndStatus(Long customerId, OrderStatus status, Pageable pageable);

	List<OrderEntity> findByWorkshopId(Long workshopId);

	List<OrderEntity> findByWorkshopIdAndStatus(Long workshopId, OrderStatus status);

	Page<OrderEntity> findByWorkshopId(Long workshopId, Pageable pageable);

	Page<OrderEntity> findByWorkshopIdAndStatus(Long workshopId, OrderStatus status, Pageable pageable);

	Page<OrderEntity> findByWorkshopIdAndOrderType(Long workshopId, OrderType orderType, Pageable pageable);

	Page<OrderEntity> findByWorkshopIdAndStatusAndOrderType(
			Long workshopId,
			OrderStatus status,
			OrderType orderType,
			Pageable pageable);

	List<OrderEntity> findByStatus(OrderStatus status);

	Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);

	List<OrderEntity> findByCreatedAtBetween(Instant from, Instant to);

	List<OrderEntity> findByCheckoutBatchIdAndCustomerId(String checkoutBatchId, Long customerId);
}
