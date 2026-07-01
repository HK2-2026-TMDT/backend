package vn.io.sanmaymac.modules.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ComplaintCreateRequest(
        @NotNull Long orderId,
        @NotBlank @Size(min = 10, max = 2000) String reason,
        @NotNull @Size(min = 1, max = 10) List<@NotBlank String> imageUrls) {
}
