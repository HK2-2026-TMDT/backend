package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;

public record CashflowResponseRecord(
        BigDecimal escrowBalance,
        BigDecimal workshopAvailable,
        BigDecimal platformRevenue) {
}
