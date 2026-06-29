package vn.io.sanmaymac.modules.bidding.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QuoteCreateRequest(
        @NotNull BigDecimal offeredPrice,
        @NotNull @Min(1) Integer estimateDays) {
}
