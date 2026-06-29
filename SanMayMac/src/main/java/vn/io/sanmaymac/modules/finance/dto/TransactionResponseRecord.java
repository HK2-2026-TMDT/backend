package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponseRecord(
	Long id,
	Long orderId,
	BigDecimal amount,
	String type,
	String direction,
	String status,
	String description,
	Instant createdAt) {
}
