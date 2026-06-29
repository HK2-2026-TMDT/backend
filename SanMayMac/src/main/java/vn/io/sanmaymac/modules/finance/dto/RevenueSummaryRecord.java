package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record RevenueSummaryRecord(String groupBy, List<RevenueSummaryItemRecord> items) {
}
