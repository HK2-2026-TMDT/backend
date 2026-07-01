package vn.io.sanmaymac.modules.complaint.dto;

import jakarta.validation.constraints.NotNull;
import vn.io.sanmaymac.common.enums.ComplaintStatus;

public record AdminUpdateComplaintStatusRequest(
        @NotNull ComplaintStatus status,
        String adminNote) {
}
