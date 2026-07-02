package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;

public record WorkshopRevenueShareRecord(
        Long workshopId,
        String workshopName,
        BigDecimal revenue,
        long orderCount) {
}
