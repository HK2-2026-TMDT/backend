package vn.io.sanmaymac.modules.review.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.enums.OrderStatus;
import vn.io.sanmaymac.common.enums.ReviewReportResolution;
import vn.io.sanmaymac.common.enums.ReviewReportStatus;
import vn.io.sanmaymac.common.enums.ReviewStatus;
import vn.io.sanmaymac.common.utils.SecurityUtils;
import vn.io.sanmaymac.modules.catalog.entity.ProductEntity;
import vn.io.sanmaymac.modules.catalog.repository.ProductRepository;
import vn.io.sanmaymac.modules.order.entity.OrderEntity;
import vn.io.sanmaymac.modules.order.repository.OrderRepository;
import vn.io.sanmaymac.modules.review.dto.AdminUpdateReviewStatusRequest;
import vn.io.sanmaymac.modules.review.dto.ResolveReportRequest;
import vn.io.sanmaymac.modules.review.dto.ReviewCreateRequest;
import vn.io.sanmaymac.modules.review.dto.ReviewReplyRequest;
import vn.io.sanmaymac.modules.review.dto.ReviewReportRequest;
import vn.io.sanmaymac.modules.review.dto.ReviewReportResponseRecord;
import vn.io.sanmaymac.modules.review.dto.ReviewResponseRecord;
import vn.io.sanmaymac.modules.review.dto.ReviewSummaryRecord;
import vn.io.sanmaymac.modules.review.dto.ReviewUpdateRequest;
import vn.io.sanmaymac.modules.review.dto.UnreviewedOrderResponseRecord;
import vn.io.sanmaymac.modules.review.entity.ReviewEntity;
import vn.io.sanmaymac.modules.review.entity.ReviewImageEntity;
import vn.io.sanmaymac.modules.review.entity.ReviewReplyEntity;
import vn.io.sanmaymac.modules.review.entity.ReviewReportEntity;
import vn.io.sanmaymac.modules.review.repository.ReviewImageRepository;
import vn.io.sanmaymac.modules.review.repository.ReviewReplyRepository;
import vn.io.sanmaymac.modules.review.repository.ReviewReportRepository;
import vn.io.sanmaymac.modules.review.repository.ReviewRepository;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;

@Service
@Transactional
public class ReviewService {
	private static final long UPDATE_WINDOW_DAYS = 7;

	private final ReviewRepository reviewRepository;
	private final ReviewImageRepository reviewImageRepository;
	private final ReviewReplyRepository reviewReplyRepository;
	private final ReviewReportRepository reviewReportRepository;
	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final ProductRepository productRepository;

	public ReviewService(
			ReviewRepository reviewRepository,
			ReviewImageRepository reviewImageRepository,
			ReviewReplyRepository reviewReplyRepository,
			ReviewReportRepository reviewReportRepository,
			OrderRepository orderRepository,
			UserRepository userRepository,
			ProductRepository productRepository) {
		this.reviewRepository = reviewRepository;
		this.reviewImageRepository = reviewImageRepository;
		this.reviewReplyRepository = reviewReplyRepository;
		this.reviewReportRepository = reviewReportRepository;
		this.orderRepository = orderRepository;
		this.userRepository = userRepository;
		this.productRepository = productRepository;
	}

	public ReviewResponseRecord createReview(ReviewCreateRequest request) {
		UserEntity user = getCurrentUser();
		OrderEntity order = orderRepository.findById(request.orderId())
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));

		if (!Objects.equals(order.getCustomer().getId(), user.getId())) {
			throw new IllegalStateException("Order does not belong to current user");
		}
		if (!OrderStatus.COMPLETED.equals(order.getStatus())) {
			throw new IllegalStateException("Order is not completed");
		}
		if (reviewRepository.existsByOrderId(order.getId())) {
			throw new IllegalStateException("Order already reviewed");
		}

		ProductEntity product = resolveProduct(request.productId());
		ReviewEntity review = ReviewEntity.builder()
				.order(order)
				.user(user)
				.workshop(order.getWorkshop())
				.product(product)
				.rating(request.rating())
				.comment(request.comment())
				.status(ReviewStatus.ACTIVE)
				.build();

		ReviewEntity saved = reviewRepository.save(review);
		saveImages(saved, request.imageUrls());
		return toResponse(saved);
	}

	public ReviewResponseRecord updateReview(Long reviewId, ReviewUpdateRequest request) {
		UserEntity user = getCurrentUser();
		ReviewEntity review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new IllegalArgumentException("Review not found"));

		if (!Objects.equals(review.getUser().getId(), user.getId())) {
			throw new IllegalStateException("No permission to update review");
		}
		if (ReviewStatus.DELETED.equals(review.getStatus())) {
			throw new IllegalStateException("Review is deleted");
		}
		if (review.getCreatedAt() != null
				&& Instant.now().isAfter(review.getCreatedAt().plus(UPDATE_WINDOW_DAYS, ChronoUnit.DAYS))) {
			throw new IllegalStateException("Review update window expired");
		}

		review.setRating(request.rating());
		review.setComment(request.comment());
		reviewRepository.save(review);

		reviewImageRepository.deleteByReviewId(review.getId());
		saveImages(review, request.imageUrls());

		return toResponse(review);
	}

	public ReviewResponseRecord deleteReview(Long reviewId) {
		UserEntity user = getCurrentUser();
		ReviewEntity review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new IllegalArgumentException("Review not found"));
		if (!Objects.equals(review.getUser().getId(), user.getId())) {
			throw new IllegalStateException("No permission to delete review");
		}
		if (ReviewStatus.DELETED.equals(review.getStatus())) {
			throw new IllegalStateException("Review is already deleted");
		}
		review.setStatus(ReviewStatus.DELETED);
		reviewRepository.save(review);
		return toResponse(review);
	}

	public Page<ReviewResponseRecord> getMyReviews(Pageable pageable) {
		UserEntity user = getCurrentUser();
		List<ReviewStatus> statuses = List.of(ReviewStatus.ACTIVE, ReviewStatus.HIDDEN);
		return reviewRepository.findByUserIdAndStatusIn(user.getId(), statuses, pageable)
				.map(this::toResponse);
	}

	public List<UnreviewedOrderResponseRecord> getUnreviewedOrders() {
		UserEntity user = getCurrentUser();
		List<OrderEntity> orders = orderRepository.findByCustomerIdAndStatus(user.getId(), OrderStatus.COMPLETED);
		List<UnreviewedOrderResponseRecord> result = new ArrayList<>();
		for (OrderEntity order : orders) {
			if (!reviewRepository.existsByOrderId(order.getId())) {
				result.add(new UnreviewedOrderResponseRecord(
						order.getId(),
						order.getWorkshop() != null ? order.getWorkshop().getId() : null,
						order.getTotalAmount()));
			}
		}
		return result;
	}

	public Page<ReviewResponseRecord> getWorkshopReviews(Integer rating, Boolean hasImages, Pageable pageable) {
		UserEntity workshop = getCurrentUser();
		List<ReviewStatus> statuses = List.of(ReviewStatus.ACTIVE, ReviewStatus.HIDDEN);
		return reviewRepository.searchWorkshopReviews(workshop.getId(), statuses, rating, hasImages, pageable)
				.map(this::toResponse);
	}

	public ReviewResponseRecord replyToReview(Long reviewId, ReviewReplyRequest request) {
		UserEntity workshop = getCurrentUser();
		ReviewEntity review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new IllegalArgumentException("Review not found"));

		if (!Objects.equals(review.getWorkshop().getId(), workshop.getId())) {
			throw new IllegalStateException("No permission to reply review");
		}
		if (ReviewStatus.DELETED.equals(review.getStatus())) {
			throw new IllegalStateException("Review is deleted");
		}

		ReviewReplyEntity reply = reviewReplyRepository.findByReviewId(reviewId)
				.orElseGet(() -> ReviewReplyEntity.builder()
						.review(review)
						.workshop(workshop)
						.build());
		reply.setContent(request.content());
		reviewReplyRepository.save(reply);
		return toResponse(review);
	}

	public ReviewReportResponseRecord reportReview(Long reviewId, ReviewReportRequest request) {
		UserEntity workshop = getCurrentUser();
		ReviewEntity review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new IllegalArgumentException("Review not found"));

		if (!Objects.equals(review.getWorkshop().getId(), workshop.getId())) {
			throw new IllegalStateException("No permission to report review");
		}

		reviewReportRepository.findByReviewIdAndWorkshopId(reviewId, workshop.getId())
				.filter(report -> ReviewReportStatus.PENDING.equals(report.getStatus()))
				.ifPresent(report -> {
					throw new IllegalStateException("Report already submitted");
				});

		ReviewReportEntity report = ReviewReportEntity.builder()
				.review(review)
				.workshop(workshop)
				.reason(request.reason())
				.status(ReviewReportStatus.PENDING)
				.build();

		return toReportResponse(reviewReportRepository.save(report));
	}

	public Page<ReviewResponseRecord> getPublicReviewsByWorkshop(
			Long workshopId,
			Integer rating,
			Boolean hasImages,
			Pageable pageable) {
		return reviewRepository.searchWorkshopReviews(
						workshopId,
						List.of(ReviewStatus.ACTIVE),
						rating,
						hasImages,
						pageable)
				.map(this::toResponse);
	}

	public Page<ReviewResponseRecord> getPublicReviewsByProduct(
			Long productId,
			Integer rating,
			Boolean hasImages,
			Pageable pageable) {
		return reviewRepository.searchProductReviews(productId, ReviewStatus.ACTIVE, rating, hasImages, pageable)
				.map(this::toResponse);
	}

	public ReviewSummaryRecord getWorkshopSummary(Long workshopId) {
		Double avg = reviewRepository.avgRatingByWorkshopIdAndStatus(workshopId, ReviewStatus.ACTIVE);
		long total = reviewRepository.countByWorkshopIdAndStatus(workshopId, ReviewStatus.ACTIVE);
		return new ReviewSummaryRecord(avg == null ? 0.0 : avg, total);
	}

	public ReviewSummaryRecord getProductSummary(Long productId) {
		Double avg = reviewRepository.avgRatingByProductIdAndStatus(productId, ReviewStatus.ACTIVE);
		long total = reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.ACTIVE);
		return new ReviewSummaryRecord(avg == null ? 0.0 : avg, total);
	}

	public Page<ReviewResponseRecord> listAllReviews(String status, Pageable pageable) {
		if (status == null || status.isBlank()) {
			return reviewRepository.findAll(pageable).map(this::toResponse);
		}
		ReviewStatus reviewStatus = ReviewStatus.valueOf(status.toUpperCase());
		return reviewRepository.findByStatus(reviewStatus, pageable).map(this::toResponse);
	}

	public ReviewResponseRecord updateReviewStatus(Long reviewId, AdminUpdateReviewStatusRequest request) {
		ReviewEntity review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new IllegalArgumentException("Review not found"));

		ReviewStatus status = ReviewStatus.valueOf(request.status().toUpperCase());
		review.setStatus(status);
		reviewRepository.save(review);
		return toResponse(review);
	}

	public List<ReviewReportResponseRecord> listReports(String status) {
		if (status == null || status.isBlank()) {
			return reviewReportRepository.findAll().stream().map(this::toReportResponse).toList();
		}
		ReviewReportStatus reportStatus = ReviewReportStatus.valueOf(status.toUpperCase());
		return reviewReportRepository.findByStatus(reportStatus).stream().map(this::toReportResponse).toList();
	}

	public ReviewReportResponseRecord resolveReport(Long reportId, ResolveReportRequest request) {
		ReviewReportEntity report = reviewReportRepository.findById(reportId)
				.orElseThrow(() -> new IllegalArgumentException("Report not found"));

		if (ReviewReportStatus.RESOLVED.equals(report.getStatus())) {
			throw new IllegalStateException("Report already resolved");
		}

		ReviewReportResolution resolution = ReviewReportResolution.valueOf(request.resolution().toUpperCase());
		report.setResolution(resolution);
		report.setAdminNote(request.adminNote());
		report.setStatus(ReviewReportStatus.RESOLVED);
		report.setResolvedAt(Instant.now());
		reviewReportRepository.save(report);

		ReviewEntity review = report.getReview();
		if (resolution == ReviewReportResolution.KEEP) {
			review.setStatus(ReviewStatus.ACTIVE);
		} else if (resolution == ReviewReportResolution.HIDE) {
			review.setStatus(ReviewStatus.HIDDEN);
		} else if (resolution == ReviewReportResolution.DELETE) {
			review.setStatus(ReviewStatus.DELETED);
		}
		reviewRepository.save(review);

		return toReportResponse(report);
	}

	private UserEntity getCurrentUser() {
		String email = SecurityUtils.getCurrentUserEmail();
		if (email == null || email.isBlank()) {
			throw new IllegalStateException("Unauthenticated");
		}
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	private ProductEntity resolveProduct(Long productId) {
		if (productId == null) {
			return null;
		}
		return productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));
	}

	private void saveImages(ReviewEntity review, List<String> imageUrls) {
		if (imageUrls == null || imageUrls.isEmpty()) {
			return;
		}
		for (String url : imageUrls) {
			ReviewImageEntity image = ReviewImageEntity.builder()
					.review(review)
					.imageUrl(url)
					.build();
			reviewImageRepository.save(image);
		}
	}


	private ReviewResponseRecord toResponse(ReviewEntity review) {
		List<String> imageUrls = reviewImageRepository.findByReviewId(review.getId())
				.stream()
				.map(ReviewImageEntity::getImageUrl)
				.toList();
		ReviewReplyEntity reply = reviewReplyRepository.findByReviewId(review.getId()).orElse(null);
		return new ReviewResponseRecord(
				review.getId(),
				review.getOrder() != null ? review.getOrder().getId() : null,
				review.getProduct() != null ? review.getProduct().getId() : null,
				review.getWorkshop() != null ? review.getWorkshop().getId() : null,
				review.getUser() != null ? review.getUser().getId() : null,
				review.getRating(),
				review.getComment(),
				review.getStatus() != null ? review.getStatus().name() : null,
				imageUrls,
				reply != null ? reply.getContent() : null,
				reply != null ? reply.getCreatedAt() : null,
				review.getCreatedAt(),
				review.getUpdatedAt());
	}

	private ReviewReportResponseRecord toReportResponse(ReviewReportEntity report) {
		return new ReviewReportResponseRecord(
				report.getId(),
				report.getReview() != null ? report.getReview().getId() : null,
				report.getWorkshop() != null ? report.getWorkshop().getId() : null,
				report.getReason(),
				report.getStatus() != null ? report.getStatus().name() : null,
				report.getResolution() != null ? report.getResolution().name() : null,
				report.getAdminNote(),
				report.getResolvedAt(),
				report.getCreatedAt());
	}
}
