package vn.io.sanmaymac.modules.user.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.modules.user.dto.AddressRequest;
import vn.io.sanmaymac.modules.user.dto.AddressResponseRecord;
import vn.io.sanmaymac.modules.user.dto.AdminUserResponseRecord;
import vn.io.sanmaymac.modules.user.dto.AdminUpdateRoleRequest;
import vn.io.sanmaymac.modules.user.dto.AdminUpdateStatusRequest;
import vn.io.sanmaymac.modules.user.dto.PortfolioItemRequest;
import vn.io.sanmaymac.modules.user.dto.PortfolioItemResponseRecord;
import vn.io.sanmaymac.modules.user.dto.ReputationResponseRecord;
import vn.io.sanmaymac.modules.user.dto.UpdateUserProfileRequest;
import vn.io.sanmaymac.modules.user.dto.UserProfileResponseRecord;
import vn.io.sanmaymac.modules.user.dto.VettingRequest;
import vn.io.sanmaymac.modules.user.dto.WorkshopKycRequest;
import vn.io.sanmaymac.modules.user.dto.WorkshopKycResponseRecord;
import vn.io.sanmaymac.modules.user.dto.WorkshopProfileRequest;
import vn.io.sanmaymac.modules.user.dto.WorkshopProfileResponseRecord;
import vn.io.sanmaymac.modules.user.dto.WorkshopPublicResponseRecord;
import vn.io.sanmaymac.modules.user.service.UserService;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserProfileResponseRecord>> getMyProfile() {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.getMyProfile()));
	}

	@GetMapping("/workshops/public")
	public ResponseEntity<ApiResponse<Page<WorkshopPublicResponseRecord>>> listPublicWorkshops(
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "false") boolean verifiedOnly,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.listPublicWorkshops(keyword, verifiedOnly, pageable)));
	}

	@GetMapping("/workshops/public/{workshopId}")
	public ResponseEntity<ApiResponse<WorkshopPublicResponseRecord>> getPublicWorkshop(
			@PathVariable Long workshopId) {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.getPublicWorkshop(workshopId)));
	}

	@GetMapping("/workshops/public/{workshopId}/portfolio")
	public ResponseEntity<ApiResponse<List<PortfolioItemResponseRecord>>> listPublicWorkshopPortfolio(
			@PathVariable Long workshopId) {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.listPublicPortfolio(workshopId)));
	}

	@PutMapping("/me")
	public ResponseEntity<ApiResponse<UserProfileResponseRecord>> updateMyProfile(
			@Valid @RequestBody UpdateUserProfileRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", userService.updateMyProfile(request)));
	}

	@GetMapping("/me/addresses")
	public ResponseEntity<ApiResponse<List<AddressResponseRecord>>> listMyAddresses() {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.listMyAddresses()));
	}

	@PostMapping("/me/addresses")
	public ResponseEntity<ApiResponse<AddressResponseRecord>> addAddress(
			@Valid @RequestBody AddressRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Created", userService.addAddress(request)));
	}

	@PutMapping("/me/addresses/{id}")
	public ResponseEntity<ApiResponse<AddressResponseRecord>> updateAddress(
			@PathVariable Long id,
			@Valid @RequestBody AddressRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", userService.updateAddress(id, request)));
	}

	@DeleteMapping("/me/addresses/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long id) {
		userService.deleteAddress(id);
		return ResponseEntity.ok(ApiResponse.success("Deleted", null));
	}

	@GetMapping("/me/favorites/workshops")
	public ResponseEntity<ApiResponse<List<UserProfileResponseRecord>>> listFavoriteWorkshops() {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.listFavoriteWorkshops()));
	}

	@PostMapping("/me/favorites/workshops/{workshopId}")
	public ResponseEntity<ApiResponse<Void>> addFavoriteWorkshop(@PathVariable Long workshopId) {
		userService.addFavoriteWorkshop(workshopId);
		return ResponseEntity.ok(ApiResponse.success("Added", null));
	}

	@DeleteMapping("/me/favorites/workshops/{workshopId}")
	public ResponseEntity<ApiResponse<Void>> removeFavoriteWorkshop(@PathVariable Long workshopId) {
		userService.removeFavoriteWorkshop(workshopId);
		return ResponseEntity.ok(ApiResponse.success("Removed", null));
	}

	@GetMapping("/me/reviews")
	public ResponseEntity<ApiResponse<List<Long>>> listMyReviewIds() {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.listMyReviewIds()));
	}

	@GetMapping("/workshop/profile")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<WorkshopProfileResponseRecord>> getWorkshopProfile() {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.getWorkshopProfile()));
	}

	@PutMapping("/workshop/profile")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<WorkshopProfileResponseRecord>> updateWorkshopProfile(
			@Valid @RequestBody WorkshopProfileRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", userService.updateWorkshopProfile(request)));
	}

	@GetMapping("/workshop/kyc")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<WorkshopKycResponseRecord>> getWorkshopKyc() {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.getWorkshopKyc()));
	}

	@PutMapping("/workshop/kyc")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<WorkshopKycResponseRecord>> submitWorkshopKyc(
			@Valid @RequestBody WorkshopKycRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Submitted", userService.submitWorkshopKyc(request)));
	}

	@GetMapping("/workshop/portfolio")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<List<PortfolioItemResponseRecord>>> listPortfolio() {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.listPortfolio()));
	}

	@PostMapping("/workshop/portfolio")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<PortfolioItemResponseRecord>> addPortfolioItem(
			@Valid @RequestBody PortfolioItemRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Created", userService.addPortfolioItem(request)));
	}

	@PutMapping("/workshop/portfolio/{id}")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<PortfolioItemResponseRecord>> updatePortfolioItem(
			@PathVariable Long id,
			@Valid @RequestBody PortfolioItemRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", userService.updatePortfolioItem(id, request)));
	}

	@DeleteMapping("/workshop/portfolio/{id}")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<Void>> deletePortfolioItem(@PathVariable Long id) {
		userService.deletePortfolioItem(id);
		return ResponseEntity.ok(ApiResponse.success("Deleted", null));
	}

	@GetMapping("/workshop/reputation")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<ReputationResponseRecord>> getReputation() {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.getReputation()));
	}

	@GetMapping("/admin/search")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<AdminUserResponseRecord>>> searchUsers(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String role,
			@RequestParam(required = false) String status,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", userService.searchUsers(keyword, role, status, pageable)));
	}

	@PutMapping("/admin/{userId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> updateUserStatus(
			@PathVariable Long userId,
			@Valid @RequestBody AdminUpdateStatusRequest request) {
		userService.updateUserStatus(userId, request);
		return ResponseEntity.ok(ApiResponse.success("Updated", null));
	}

	@PutMapping("/admin/{userId}/role")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> updateUserRole(
			@PathVariable Long userId,
			@Valid @RequestBody AdminUpdateRoleRequest request) {
		userService.updateUserRole(userId, request);
		return ResponseEntity.ok(ApiResponse.success("Updated", null));
	}

	@PutMapping("/admin/{workshopId}/vetting")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> vettingWorkshop(
			@PathVariable Long workshopId,
			@Valid @RequestBody VettingRequest request) {
		userService.vettingWorkshop(workshopId, request);
		return ResponseEntity.ok(ApiResponse.success("Updated", null));
	}
}
