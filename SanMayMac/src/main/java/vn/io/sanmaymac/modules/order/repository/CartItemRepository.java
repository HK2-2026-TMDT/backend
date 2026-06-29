package vn.io.sanmaymac.modules.order.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.order.entity.CartItemEntity;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    List<CartItemEntity> findByCartId(Long cartId);

    Optional<CartItemEntity> findByCartIdAndVariantId(Long cartId, Long variantId);

    Optional<CartItemEntity> findByIdAndCartId(Long id, Long cartId);

    void deleteByCartId(Long cartId);
}
