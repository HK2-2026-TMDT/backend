package vn.io.sanmaymac.modules.shipping.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.modules.shipping.dto.ShippingQuoteRequest;
import vn.io.sanmaymac.modules.shipping.dto.ShippingQuoteResponseRecord;
import vn.io.sanmaymac.modules.shipping.service.ShippingService;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {
	private final ShippingService shippingService;

	public ShippingController(ShippingService shippingService) {
		this.shippingService = shippingService;
	}

	@PostMapping("/quote")
	public ResponseEntity<ApiResponse<ShippingQuoteResponseRecord>> quote(
			@Valid @RequestBody ShippingQuoteRequest request) {
		return ResponseEntity.ok(ApiResponse.success("OK", shippingService.quote(request)));
	}
}
