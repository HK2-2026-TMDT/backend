package vn.io.sanmaymac.modules.review.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.review.entity.ReviewReplyEntity;

public interface ReviewReplyRepository extends JpaRepository<ReviewReplyEntity, Long> {
    Optional<ReviewReplyEntity> findByReviewId(Long reviewId);
}
