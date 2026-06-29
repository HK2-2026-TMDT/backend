package vn.io.sanmaymac.modules.catalog.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.catalog.entity.ProductFavoriteEntity;

public interface ProductFavoriteRepository extends JpaRepository<ProductFavoriteEntity, Long> {
    List<ProductFavoriteEntity> findByCustomerId(Long customerId);

    Optional<ProductFavoriteEntity> findByCustomerIdAndProductId(Long customerId, Long productId);

    boolean existsByCustomerIdAndProductId(Long customerId, Long productId);

    void deleteByProductId(Long productId);
}