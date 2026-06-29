package vn.io.sanmaymac.modules.catalog.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.catalog.entity.ProductVariantEntity;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {
	List<ProductVariantEntity> findByProductId(Long productId);

	void deleteByProductId(Long productId);
}
