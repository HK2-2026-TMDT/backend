package vn.io.sanmaymac.modules.catalog.dto;

import java.math.BigDecimal;

public record CatalogResponseRecord(Long productId, String name, BigDecimal basePrice) {
}
