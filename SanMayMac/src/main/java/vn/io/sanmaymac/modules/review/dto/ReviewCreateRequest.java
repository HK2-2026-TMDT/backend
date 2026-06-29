package vn.io.sanmaymac.modules.review.dto;

import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequest(
        @NotNull Long orderId,
        Long productId,
        @NotNull @Min(1) @Max(5) Integer rating,
        String comment,
        List<String> imageUrls) {
}
