package vn.io.sanmaymac.modules.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisputeRequestInfoRequest(
        @NotBlank @Size(min = 5, max = 2000) String message) {
}
