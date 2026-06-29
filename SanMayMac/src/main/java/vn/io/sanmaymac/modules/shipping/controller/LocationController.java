package vn.io.sanmaymac.modules.shipping.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.modules.shipping.dto.DistrictRecord;
import vn.io.sanmaymac.modules.shipping.dto.ProvinceRecord;
import vn.io.sanmaymac.modules.shipping.dto.WardRecord;
import vn.io.sanmaymac.modules.shipping.service.GhnService;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
	private final GhnService ghnService;

	public LocationController(GhnService ghnService) {
		this.ghnService = ghnService;
	}

	@GetMapping("/provinces")
	public ResponseEntity<ApiResponse<List<ProvinceRecord>>> listProvinces() {
		List<ProvinceRecord> provinces = ghnService.listProvinces();
		String message = provinces.isEmpty()
				? (ghnService.getLastError() != null ? ghnService.getLastError() : "Không có dữ liệu tỉnh/thành")
				: "OK";
		return ResponseEntity.ok(ApiResponse.success(message, provinces));
	}

	@GetMapping("/districts")
	public ResponseEntity<ApiResponse<List<DistrictRecord>>> listDistricts(
			@RequestParam int provinceId) {
		return ResponseEntity.ok(ApiResponse.success("OK", ghnService.listDistricts(provinceId)));
	}

	@GetMapping("/wards")
	public ResponseEntity<ApiResponse<List<WardRecord>>> listWards(
			@RequestParam int districtId) {
		return ResponseEntity.ok(ApiResponse.success("OK", ghnService.listWards(districtId)));
	}
}
