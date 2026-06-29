package vn.io.sanmaymac.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import vn.io.sanmaymac.common.enums.Role;

public record AdminUpdateRoleRequest(@NotNull Role role) {
}
