package vn.io.sanmaymac.modules.complaint.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.common.enums.ComplaintStatus;
import vn.io.sanmaymac.modules.complaint.entity.ComplaintEntity;

public interface ComplaintRepository extends JpaRepository<ComplaintEntity, Long> {
    boolean existsByOrderId(Long orderId);

    Optional<ComplaintEntity> findByOrderId(Long orderId);

    Page<ComplaintEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    Page<ComplaintEntity> findByStatusOrderByCreatedAtDesc(ComplaintStatus status, Pageable pageable);

    Page<ComplaintEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
