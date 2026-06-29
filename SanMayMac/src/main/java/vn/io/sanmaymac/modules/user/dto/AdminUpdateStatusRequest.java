package vn.io.sanmaymac.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import vn.io.sanmaymac.common.enums.UserStatus;

public record AdminUpdateStatusRequest(@NotNull UserStatus status) {
}
