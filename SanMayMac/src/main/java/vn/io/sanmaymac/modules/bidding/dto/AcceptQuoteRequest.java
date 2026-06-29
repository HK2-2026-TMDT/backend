package vn.io.sanmaymac.modules.bidding.dto;

import jakarta.validation.constraints.NotNull;

public record AcceptQuoteRequest(@NotNull Long addressId) {
}
