package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AiTokenPurchaseRequest(
        @NotNull BigDecimal amount,
        @NotNull @Min(1) Integer tokenCount) {
}
