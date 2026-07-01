package vn.io.sanmaymac.modules.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ViolationRecordRequest(
        @NotBlank @Size(min = 10, max = 2000) String description) {
}
