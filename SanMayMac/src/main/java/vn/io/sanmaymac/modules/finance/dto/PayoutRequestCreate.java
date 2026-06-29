package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

public record PayoutRequestCreate(@NotNull BigDecimal amount) {
}
