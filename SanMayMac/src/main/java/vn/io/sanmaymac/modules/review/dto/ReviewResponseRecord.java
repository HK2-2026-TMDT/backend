package vn.io.sanmaymac.modules.review.dto;

import java.time.Instant;
import java.util.List;

public record ReviewResponseRecord(
	Long id,
	Long orderId,
	Long productId,
	Long workshopId,
	Long userId,
	Integer rating,
	String comment,
	String status,
	List<String> imageUrls,
	String replyContent,
	Instant replyAt,
	Instant createdAt,
	Instant updatedAt) {
}
