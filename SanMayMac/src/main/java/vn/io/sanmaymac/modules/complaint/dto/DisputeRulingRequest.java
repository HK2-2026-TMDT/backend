package vn.io.sanmaymac.modules.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisputeRulingRequest(
        @NotBlank @Size(min = 10, max = 5000) String ruling) {
}
