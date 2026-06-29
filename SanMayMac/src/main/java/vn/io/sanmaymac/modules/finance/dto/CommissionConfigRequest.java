package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CommissionConfigRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal commissionRate) {
}
