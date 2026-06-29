package vn.io.sanmaymac.modules.review.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.io.sanmaymac.modules.review.entity.ReviewImageEntity;

public interface ReviewImageRepository extends JpaRepository<ReviewImageEntity, Long> {
    void deleteByReviewId(Long reviewId);

    List<ReviewImageEntity> findByReviewId(Long reviewId);

    @Query("select distinct ri.review.id from ReviewImageEntity ri where ri.review.id in :reviewIds")
    List<Long> findReviewIdsWithImages(@Param("reviewIds") List<Long> reviewIds);
}
