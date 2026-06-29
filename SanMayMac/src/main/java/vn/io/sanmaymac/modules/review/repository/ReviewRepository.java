package vn.io.sanmaymac.modules.review.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.io.sanmaymac.common.enums.ReviewStatus;
import vn.io.sanmaymac.modules.review.entity.ReviewEntity;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
	List<ReviewEntity> findByUserId(Long userId);

	List<ReviewEntity> findByWorkshopId(Long workshopId);

	boolean existsByOrderId(Long orderId);

	List<ReviewEntity> findByUserIdAndStatus(Long userId, ReviewStatus status);

	List<ReviewEntity> findByUserIdAndStatusIn(Long userId, List<ReviewStatus> statuses);

	Page<ReviewEntity> findByUserIdAndStatusIn(Long userId, List<ReviewStatus> statuses, Pageable pageable);

	List<ReviewEntity> findByWorkshopIdAndStatus(Long workshopId, ReviewStatus status);

	List<ReviewEntity> findByWorkshopIdAndStatusIn(Long workshopId, List<ReviewStatus> statuses);

	    @Query("select r from ReviewEntity r where r.workshop.id = :workshopId and r.status in :statuses "
		    + "and (:rating is null or r.rating = :rating) "
		    + "and (:hasImages is null or exists (select 1 from ReviewImageEntity ri where ri.review.id = r.id))")
	    Page<ReviewEntity> searchWorkshopReviews(
		    @Param("workshopId") Long workshopId,
		    @Param("statuses") List<ReviewStatus> statuses,
		    @Param("rating") Integer rating,
		    @Param("hasImages") Boolean hasImages,
		    Pageable pageable);

	List<ReviewEntity> findByWorkshopIdAndRatingAndStatus(Long workshopId, Integer rating, ReviewStatus status);

	List<ReviewEntity> findByProductIdAndStatus(Long productId, ReviewStatus status);

	    @Query("select r from ReviewEntity r where r.product.id = :productId and r.status = :status "
		    + "and (:rating is null or r.rating = :rating) "
		    + "and (:hasImages is null or exists (select 1 from ReviewImageEntity ri where ri.review.id = r.id))")
	    Page<ReviewEntity> searchProductReviews(
		    @Param("productId") Long productId,
		    @Param("status") ReviewStatus status,
		    @Param("rating") Integer rating,
		    @Param("hasImages") Boolean hasImages,
		    Pageable pageable);

	    Page<ReviewEntity> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

	    Page<ReviewEntity> findByWorkshopIdAndStatus(Long workshopId, ReviewStatus status, Pageable pageable);

	    Page<ReviewEntity> findByWorkshopIdAndRatingAndStatus(Long workshopId, Integer rating, ReviewStatus status, Pageable pageable);

	@Query("select avg(r.rating) from ReviewEntity r where r.workshop.id = :workshopId and r.status = :status")
	Double avgRatingByWorkshopIdAndStatus(@Param("workshopId") Long workshopId, @Param("status") ReviewStatus status);

	@Query("select count(r.id) from ReviewEntity r where r.workshop.id = :workshopId and r.status = :status")
	long countByWorkshopIdAndStatus(@Param("workshopId") Long workshopId, @Param("status") ReviewStatus status);

	@Query("select avg(r.rating) from ReviewEntity r where r.product.id = :productId and r.status = :status")
	Double avgRatingByProductIdAndStatus(@Param("productId") Long productId, @Param("status") ReviewStatus status);

	@Query("select count(r.id) from ReviewEntity r where r.product.id = :productId and r.status = :status")
	long countByProductIdAndStatus(@Param("productId") Long productId, @Param("status") ReviewStatus status);

	Page<ReviewEntity> findByStatus(ReviewStatus status, Pageable pageable);
}
