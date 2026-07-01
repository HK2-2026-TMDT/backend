package vn.io.sanmaymac.modules.complaint.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.common.enums.DisputeStatus;
import vn.io.sanmaymac.modules.complaint.entity.DisputeEntity;

public interface DisputeRepository extends JpaRepository<DisputeEntity, Long> {
    Optional<DisputeEntity> findByComplaintId(Long complaintId);

    Page<DisputeEntity> findByStatusOrderByCreatedAtDesc(DisputeStatus status, Pageable pageable);

    Page<DisputeEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
