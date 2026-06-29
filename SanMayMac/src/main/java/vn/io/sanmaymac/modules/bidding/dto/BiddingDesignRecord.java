package vn.io.sanmaymac.modules.bidding.dto;

import java.time.Instant;

public record BiddingDesignRecord(
		Long id,
		String name,
		String frontDesignUrl,
		String backDesignUrl,
		Instant createdAt) {
}
