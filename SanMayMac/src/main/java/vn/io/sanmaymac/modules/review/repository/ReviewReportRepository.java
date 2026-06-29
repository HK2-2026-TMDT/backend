package vn.io.sanmaymac.modules.review.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.common.enums.ReviewReportStatus;
import vn.io.sanmaymac.modules.review.entity.ReviewReportEntity;

public interface ReviewReportRepository extends JpaRepository<ReviewReportEntity, Long> {
    Optional<ReviewReportEntity> findByReviewIdAndWorkshopId(Long reviewId, Long workshopId);

    List<ReviewReportEntity> findByStatus(ReviewReportStatus status);
}
