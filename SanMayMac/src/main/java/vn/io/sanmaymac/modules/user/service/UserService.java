package vn.io.sanmaymac.modules.user.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityNotFoundException;
import vn.io.sanmaymac.common.enums.KycStatus;
import vn.io.sanmaymac.common.enums.Role;
import vn.io.sanmaymac.common.enums.UserStatus;
import vn.io.sanmaymac.common.service.MediaStorageService;
import vn.io.sanmaymac.modules.review.entity.ReviewEntity;
import vn.io.sanmaymac.modules.review.repository.ReviewRepository;
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
import vn.io.sanmaymac.modules.user.entity.UserAddressEntity;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.entity.WorkshopFavoriteEntity;
import vn.io.sanmaymac.modules.user.entity.WorkshopKycDocumentEntity;
import vn.io.sanmaymac.modules.user.entity.WorkshopPortfolioEntity;
import vn.io.sanmaymac.modules.user.entity.WorkshopProfileEntity;
import vn.io.sanmaymac.modules.user.repository.UserAddressRepository;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopFavoriteRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopKycDocumentRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopPortfolioRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopProfileRepository;

@Service
@Transactional
public class UserService {
	private final UserRepository userRepository;
	private final UserAddressRepository userAddressRepository;
	private final WorkshopProfileRepository workshopProfileRepository;
	private final WorkshopKycDocumentRepository workshopKycDocumentRepository;
	private final WorkshopPortfolioRepository workshopPortfolioRepository;
	private final WorkshopFavoriteRepository workshopFavoriteRepository;
	private final ReviewRepository reviewRepository;
	private final MediaStorageService mediaStorageService;

	public UserService(
			UserRepository userRepository,
			UserAddressRepository userAddressRepository,
			WorkshopProfileRepository workshopProfileRepository,
			WorkshopKycDocumentRepository workshopKycDocumentRepository,
			WorkshopPortfolioRepository workshopPortfolioRepository,
			WorkshopFavoriteRepository workshopFavoriteRepository,
			ReviewRepository reviewRepository,
			MediaStorageService mediaStorageService) {
		this.userRepository = userRepository;
		this.userAddressRepository = userAddressRepository;
		this.workshopProfileRepository = workshopProfileRepository;
		this.workshopKycDocumentRepository = workshopKycDocumentRepository;
		this.workshopPortfolioRepository = workshopPortfolioRepository;
		this.workshopFavoriteRepository = workshopFavoriteRepository;
		this.reviewRepository = reviewRepository;
		this.mediaStorageService = mediaStorageService;
	}

	public UserProfileResponseRecord getMyProfile() {
		UserEntity user = getCurrentUser();
		return toUserProfile(user);
	}

	public Page<WorkshopPublicResponseRecord> listPublicWorkshops(String keyword, boolean verifiedOnly, Pageable pageable) {
		return workshopProfileRepository
				.findPublicWorkshops(keyword, verifiedOnly, pageable)
				.map(this::toWorkshopPublicResponse);
	}

	public List<PortfolioItemResponseRecord> listPublicPortfolio(Long workshopId) {
		UserEntity workshop = userRepository.findById(workshopId)
				.orElseThrow(() -> new EntityNotFoundException("Workshop not found"));
		if (!Role.WORKSHOP.equals(workshop.getRole())) {
			throw new EntityNotFoundException("Workshop not found");
		}
		return workshopPortfolioRepository.findByWorkshopId(workshopId)
				.stream()
				.sorted(Comparator.comparing(WorkshopPortfolioEntity::getId))
				.map(this::toPortfolioResponse)
				.collect(Collectors.toList());
	}

	public WorkshopPublicResponseRecord getPublicWorkshop(Long workshopId) {
		WorkshopProfileEntity profile = workshopProfileRepository.findByUserId(workshopId)
				.orElseThrow(() -> new EntityNotFoundException("Workshop not found"));
		if (!UserStatus.ACTIVE.equals(profile.getUser().getStatus())) {
			throw new EntityNotFoundException("Workshop not found");
		}
		return toWorkshopPublicResponse(profile);
	}

	public UserProfileResponseRecord updateMyProfile(UpdateUserProfileRequest request) {
		UserEntity user = getCurrentUser();
		user.setFullName(request.fullName());
		user.setPhoneNumber(request.phoneNumber());
		user.setAvatarUrl(request.avatarUrl());
		userRepository.save(user);
		return toUserProfile(user);
	}

	public List<AddressResponseRecord> listMyAddresses() {
		UserEntity user = getCurrentUser();
		return userAddressRepository.findByUserId(user.getId())
				.stream()
				.map(this::toAddressResponse)
				.collect(Collectors.toList());
	}

	public AddressResponseRecord addAddress(AddressRequest request) {
		UserEntity user = getCurrentUser();
		if (request.isDefault()) {
			unsetDefaultAddress(user.getId());
		}
		UserAddressEntity entity = UserAddressEntity.builder()
				.user(user)
				.receiverName(request.receiverName())
				.phone(request.phone())
				.detailedAddress(request.detailedAddress())
				.provinceId(request.provinceId())
				.districtId(request.districtId())
				.wardCode(request.wardCode())
				.provinceName(request.provinceName())
				.districtName(request.districtName())
				.wardName(request.wardName())
				.isDefault(request.isDefault())
				.build();
		return toAddressResponse(userAddressRepository.save(entity));
	}

	public AddressResponseRecord updateAddress(Long id, AddressRequest request) {
		UserEntity user = getCurrentUser();
		UserAddressEntity entity = userAddressRepository.findByIdAndUserId(id, user.getId())
				.orElseThrow(() -> new EntityNotFoundException("Address not found"));
		if (request.isDefault()) {
			unsetDefaultAddress(user.getId());
		}
		entity.setReceiverName(request.receiverName());
		entity.setPhone(request.phone());
		entity.setDetailedAddress(request.detailedAddress());
		entity.setProvinceId(request.provinceId());
		entity.setDistrictId(request.districtId());
		entity.setWardCode(request.wardCode());
		entity.setProvinceName(request.provinceName());
		entity.setDistrictName(request.districtName());
		entity.setWardName(request.wardName());
		entity.setIsDefault(request.isDefault());
		return toAddressResponse(userAddressRepository.save(entity));
	}

	public void deleteAddress(Long id) {
		UserEntity user = getCurrentUser();
		UserAddressEntity entity = userAddressRepository.findByIdAndUserId(id, user.getId())
				.orElseThrow(() -> new EntityNotFoundException("Address not found"));
		userAddressRepository.delete(entity);
	}

	public List<UserProfileResponseRecord> listFavoriteWorkshops() {
		UserEntity user = getCurrentUser();
		return workshopFavoriteRepository.findByCustomerId(user.getId())
				.stream()
				.map(WorkshopFavoriteEntity::getWorkshop)
				.map(this::toUserProfile)
				.collect(Collectors.toList());
	}

	public void addFavoriteWorkshop(Long workshopId) {
		UserEntity user = getCurrentUser();
		UserEntity workshop = getUserById(workshopId);
		if (!Role.WORKSHOP.equals(workshop.getRole())) {
			throw new IllegalArgumentException("Target user is not a workshop");
		}
		Optional<WorkshopFavoriteEntity> existing = workshopFavoriteRepository
				.findByCustomerIdAndWorkshopId(user.getId(), workshopId);
		if (existing.isPresent()) {
			return;
		}
		WorkshopFavoriteEntity entity = WorkshopFavoriteEntity.builder()
				.customer(user)
				.workshop(workshop)
				.build();
		workshopFavoriteRepository.save(entity);
	}

	public void removeFavoriteWorkshop(Long workshopId) {
		UserEntity user = getCurrentUser();
		workshopFavoriteRepository.findByCustomerIdAndWorkshopId(user.getId(), workshopId)
				.ifPresent(workshopFavoriteRepository::delete);
	}

	public List<Long> listMyReviewIds() {
		UserEntity user = getCurrentUser();
		return reviewRepository.findByUserId(user.getId())
				.stream()
				.map(ReviewEntity::getId)
				.collect(Collectors.toList());
	}

	public WorkshopProfileResponseRecord getWorkshopProfile() {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		WorkshopProfileEntity profile = workshopProfileRepository.findByUserId(workshop.getId())
				.orElseGet(() -> WorkshopProfileEntity.builder().user(workshop).build());
		return toWorkshopProfileResponse(profile);
	}

	public Long getCurrentWorkshopId() {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		return workshop.getId();
	}

	public WorkshopProfileResponseRecord uploadWorkshopAvatar(MultipartFile avatar) {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		workshop.setAvatarUrl(mediaStorageService.store(avatar, "workshop-avatar"));
		userRepository.save(workshop);
		return getWorkshopProfile();
	}

	public WorkshopProfileResponseRecord uploadWorkshopLogo(MultipartFile logo) {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		WorkshopProfileEntity profile = workshopProfileRepository.findByUserId(workshop.getId())
				.orElseGet(() -> WorkshopProfileEntity.builder().user(workshop).build());
		profile.setLogoUrl(mediaStorageService.store(logo, "workshop-logo"));
		workshopProfileRepository.save(profile);
		return toWorkshopProfileResponse(profile);
	}

	public WorkshopProfileResponseRecord updateWorkshopProfile(WorkshopProfileRequest request) {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		WorkshopProfileEntity profile = workshopProfileRepository.findByUserId(workshop.getId())
				.orElseGet(() -> WorkshopProfileEntity.builder().user(workshop).build());
		profile.setShopName(request.shopName());
		profile.setWorkshopAddress(request.workshopAddress());
		profile.setProductionCapacity(request.productionCapacity());
		profile.setDescription(request.description());
		workshopProfileRepository.save(profile);
		return toWorkshopProfileResponse(profile);
	}

	public WorkshopKycResponseRecord uploadWorkshopKycDocument(MultipartFile licenseFile) {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		WorkshopKycDocumentEntity entity = workshopKycDocumentRepository.findByUserId(workshop.getId())
				.orElseGet(() -> WorkshopKycDocumentEntity.builder().user(workshop).build());
		entity.setLicenseUrl(mediaStorageService.store(licenseFile, "workshop-kyc"));
		entity.setStatus(KycStatus.PENDING);
		entity.setAdminNote(null);
		return toWorkshopKycResponse(workshopKycDocumentRepository.save(entity));
	}

	public WorkshopKycResponseRecord getWorkshopKyc() {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		WorkshopKycDocumentEntity entity = workshopKycDocumentRepository.findByUserId(workshop.getId())
				.orElseGet(() -> WorkshopKycDocumentEntity.builder().user(workshop).status(KycStatus.PENDING).build());
		return toWorkshopKycResponse(entity);
	}

	public WorkshopKycResponseRecord submitWorkshopKyc(WorkshopKycRequest request) {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		WorkshopKycDocumentEntity entity = workshopKycDocumentRepository.findByUserId(workshop.getId())
				.orElseGet(() -> WorkshopKycDocumentEntity.builder().user(workshop).build());
		entity.setTaxCode(request.taxCode());
		entity.setLicenseUrl(request.licenseUrl());
		entity.setStatus(KycStatus.PENDING);
		entity.setAdminNote(null);
		return toWorkshopKycResponse(workshopKycDocumentRepository.save(entity));
	}

	public List<PortfolioItemResponseRecord> listPortfolio() {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		return workshopPortfolioRepository.findByWorkshopId(workshop.getId())
				.stream()
				.sorted(Comparator.comparing(WorkshopPortfolioEntity::getId))
				.map(this::toPortfolioResponse)
				.collect(Collectors.toList());
	}

	public PortfolioItemResponseRecord addPortfolioItem(PortfolioItemRequest request) {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		WorkshopPortfolioEntity entity = WorkshopPortfolioEntity.builder()
				.workshop(workshop)
				.title(request.title())
				.imageUrl(request.imageUrl())
				.description(request.description())
				.build();
		return toPortfolioResponse(workshopPortfolioRepository.save(entity));
	}

	public PortfolioItemResponseRecord addPortfolioItemWithUpload(String title, String description, MultipartFile image) {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		WorkshopPortfolioEntity entity = WorkshopPortfolioEntity.builder()
				.workshop(workshop)
				.title(title)
				.description(description)
				.imageUrl(mediaStorageService.store(image, "portfolio"))
				.build();
		return toPortfolioResponse(workshopPortfolioRepository.save(entity));
	}

	public PortfolioItemResponseRecord updatePortfolioItem(Long id, PortfolioItemRequest request) {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		WorkshopPortfolioEntity entity = workshopPortfolioRepository.findByIdAndWorkshopId(id, workshop.getId())
				.orElseThrow(() -> new EntityNotFoundException("Portfolio item not found"));
		entity.setTitle(request.title());
		entity.setImageUrl(request.imageUrl());
		entity.setDescription(request.description());
		return toPortfolioResponse(workshopPortfolioRepository.save(entity));
	}

	public void deletePortfolioItem(Long id) {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		WorkshopPortfolioEntity entity = workshopPortfolioRepository.findByIdAndWorkshopId(id, workshop.getId())
				.orElseThrow(() -> new EntityNotFoundException("Portfolio item not found"));
		workshopPortfolioRepository.delete(entity);
	}

	public ReputationResponseRecord getReputation() {
		UserEntity workshop = getCurrentUser();
		ensureWorkshop(workshop);
		List<ReviewEntity> reviews = reviewRepository.findByWorkshopId(workshop.getId());
		if (reviews.isEmpty()) {
			return new ReputationResponseRecord(0.0, 0);
		}
		double avg = reviews.stream()
				.mapToInt(review -> review.getRating() == null ? 0 : review.getRating())
				.average()
				.orElse(0.0);
		return new ReputationResponseRecord(avg, reviews.size());
	}

	    public Page<AdminUserResponseRecord> searchUsers(String keyword, String role, String status, Pageable pageable) {
		List<UserEntity> users = userRepository.findAll();
		List<UserEntity> filtered = users.stream()
			.filter(user -> keyword == null || keyword.isBlank()
				|| user.getEmail() != null && user.getEmail().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))
				|| user.getFullName() != null && user.getFullName().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)))
			.filter(user -> role == null || role.isBlank()
				|| user.getRole() != null && user.getRole().name().equalsIgnoreCase(role))
			.filter(user -> status == null || status.isBlank()
				|| user.getStatus() != null && user.getStatus().name().equalsIgnoreCase(status))
			.sorted(Comparator.comparing(UserEntity::getId))
			.collect(Collectors.toList());

		int start = Math.toIntExact(pageable.getOffset());
		int end = Math.min(start + pageable.getPageSize(), filtered.size());
		List<AdminUserResponseRecord> content = start >= end
			? List.of()
			: filtered.subList(start, end).stream().map(this::toAdminUserResponse).collect(Collectors.toList());

		return new PageImpl<>(content, pageable, filtered.size());
	    }

	private AdminUserResponseRecord toAdminUserResponse(UserEntity user) {
		return new AdminUserResponseRecord(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getPhoneNumber(),
				user.getAvatarUrl(),
				user.getRole() != null ? user.getRole().name() : null,
				user.getStatus() != null ? user.getStatus().name() : null,
				user.getCreatedAt());
	}

	public void updateUserStatus(Long userId, AdminUpdateStatusRequest request) {
		UserEntity user = getUserById(userId);
		user.setStatus(request.status());
		userRepository.save(user);
	}

	public void updateUserRole(Long userId, AdminUpdateRoleRequest request) {
		UserEntity user = getUserById(userId);
		user.setRole(request.role());
		userRepository.save(user);
	}

	public void vettingWorkshop(Long workshopId, VettingRequest request) {
		UserEntity workshop = getUserById(workshopId);
		if (!Role.WORKSHOP.equals(workshop.getRole())) {
			throw new IllegalArgumentException("Target user is not a workshop");
		}
		WorkshopProfileEntity profile = workshopProfileRepository.findByUserId(workshop.getId())
				.orElseGet(() -> WorkshopProfileEntity.builder().user(workshop).build());
		profile.setIsVerified(request.approved());
		workshopProfileRepository.save(profile);

		WorkshopKycDocumentEntity kyc = workshopKycDocumentRepository.findByUserId(workshop.getId())
				.orElseGet(() -> WorkshopKycDocumentEntity.builder().user(workshop).build());
		kyc.setStatus(request.approved() ? KycStatus.APPROVED : KycStatus.REJECTED);
		kyc.setAdminNote(request.adminNote());
		workshopKycDocumentRepository.save(kyc);
	}

	private void unsetDefaultAddress(Long userId) {
		List<UserAddressEntity> addresses = userAddressRepository.findByUserId(userId);
		for (UserAddressEntity address : addresses) {
			if (Boolean.TRUE.equals(address.getIsDefault())) {
				address.setIsDefault(false);
				userAddressRepository.save(address);
			}
		}
	}

	private UserEntity getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null) {
			throw new IllegalStateException("Unauthenticated");
		}
		String email = authentication.getName();
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
	}

	private UserEntity getUserById(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
	}

	private void ensureWorkshop(UserEntity user) {
		if (!Role.WORKSHOP.equals(user.getRole())) {
			throw new IllegalStateException("User is not a workshop");
		}
	}

	private UserProfileResponseRecord toUserProfile(UserEntity user) {
		return new UserProfileResponseRecord(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getPhoneNumber(),
				user.getAvatarUrl());
	}

	private AddressResponseRecord toAddressResponse(UserAddressEntity entity) {
		String fullAddress = buildFullAddress(entity);
		return new AddressResponseRecord(
				entity.getId(),
				entity.getReceiverName(),
				entity.getPhone(),
				entity.getDetailedAddress(),
				entity.getProvinceId(),
				entity.getDistrictId(),
				entity.getWardCode(),
				entity.getProvinceName(),
				entity.getDistrictName(),
				entity.getWardName(),
				fullAddress,
				Boolean.TRUE.equals(entity.getIsDefault()));
	}

	private String buildFullAddress(UserAddressEntity entity) {
		StringBuilder sb = new StringBuilder();
		if (entity.getDetailedAddress() != null && !entity.getDetailedAddress().isBlank()) {
			sb.append(entity.getDetailedAddress());
		}
		appendPart(sb, entity.getWardName());
		appendPart(sb, entity.getDistrictName());
		appendPart(sb, entity.getProvinceName());
		return sb.toString();
	}

	private void appendPart(StringBuilder sb, String part) {
		if (part == null || part.isBlank()) {
			return;
		}
		if (!sb.isEmpty()) {
			sb.append(", ");
		}
		sb.append(part);
	}

	private WorkshopProfileResponseRecord toWorkshopProfileResponse(WorkshopProfileEntity profile) {
		return new WorkshopProfileResponseRecord(
				profile.getShopName(),
				profile.getLogoUrl(),
				profile.getWorkshopAddress(),
				profile.getProductionCapacity(),
				profile.getDescription(),
				Boolean.TRUE.equals(profile.getIsVerified()),
				profile.getRatingAvg() == null ? 0.0 : profile.getRatingAvg());
	}

	private WorkshopPublicResponseRecord toWorkshopPublicResponse(WorkshopProfileEntity profile) {
		UserEntity user = profile.getUser();
		return new WorkshopPublicResponseRecord(
				user.getId(),
				user.getFullName(),
				user.getAvatarUrl(),
				profile.getShopName(),
				profile.getLogoUrl(),
				profile.getWorkshopAddress(),
				profile.getProductionCapacity(),
				profile.getDescription(),
				Boolean.TRUE.equals(profile.getIsVerified()),
				profile.getRatingAvg() == null ? 0.0 : profile.getRatingAvg());
	}

	private WorkshopKycResponseRecord toWorkshopKycResponse(WorkshopKycDocumentEntity entity) {
		return new WorkshopKycResponseRecord(
				entity.getTaxCode(),
				entity.getLicenseUrl(),
				entity.getStatus(),
				entity.getAdminNote());
	}

	private PortfolioItemResponseRecord toPortfolioResponse(WorkshopPortfolioEntity entity) {
		return new PortfolioItemResponseRecord(
				entity.getId(),
				entity.getTitle(),
				entity.getImageUrl(),
				entity.getDescription());
	}
}
