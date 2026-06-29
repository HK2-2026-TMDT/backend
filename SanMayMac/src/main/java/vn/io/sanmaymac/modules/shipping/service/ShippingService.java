package vn.io.sanmaymac.modules.shipping.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.modules.order.entity.CartEntity;
import vn.io.sanmaymac.modules.order.entity.CartItemEntity;
import vn.io.sanmaymac.modules.order.repository.CartItemRepository;
import vn.io.sanmaymac.modules.order.repository.CartRepository;
import vn.io.sanmaymac.modules.shipping.dto.ShippingQuoteRequest;
import vn.io.sanmaymac.modules.shipping.dto.ShippingQuoteResponseRecord;
import vn.io.sanmaymac.modules.user.entity.UserAddressEntity;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.repository.UserAddressRepository;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ShippingService {
	private final GhnService ghnService;
	private final UserRepository userRepository;
	private final UserAddressRepository userAddressRepository;
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;

	public ShippingService(
			GhnService ghnService,
			UserRepository userRepository,
			UserAddressRepository userAddressRepository,
			CartRepository cartRepository,
			CartItemRepository cartItemRepository) {
		this.ghnService = ghnService;
		this.userRepository = userRepository;
		this.userAddressRepository = userAddressRepository;
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
	}

	public ShippingQuoteResponseRecord quote(ShippingQuoteRequest request) {
		UserEntity user = getCurrentUser();
		UserAddressEntity address = userAddressRepository.findByIdAndUserId(request.addressId(), user.getId())
				.orElseThrow(() -> new IllegalArgumentException("Address not found"));
		int totalQuantity = countCartQuantity(user);
		return ghnService.calculateFee(address, totalQuantity);
	}

	private int countCartQuantity(UserEntity user) {
		CartEntity cart = cartRepository.findByCustomerId(user.getId()).orElse(null);
		if (cart == null) {
			return 1;
		}
		List<CartItemEntity> items = cartItemRepository.findByCartId(cart.getId());
		return items.stream()
				.mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 1)
				.sum();
	}

	private UserEntity getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null) {
			throw new IllegalStateException("Unauthenticated");
		}
		return userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
	}
}
