package vn.io.sanmaymac.modules.complaint.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.complaint.entity.ComplaintImageEntity;

public interface ComplaintImageRepository extends JpaRepository<ComplaintImageEntity, Long> {
    List<ComplaintImageEntity> findByComplaintIdOrderByIdAsc(Long complaintId);
}
