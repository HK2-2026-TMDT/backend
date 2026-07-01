package vn.io.sanmaymac.modules.complaint.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.complaint.entity.WorkshopViolationRecordEntity;

public interface WorkshopViolationRecordRepository extends JpaRepository<WorkshopViolationRecordEntity, Long> {
}
