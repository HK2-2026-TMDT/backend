package vn.io.sanmaymac.modules.catalog.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.catalog.entity.ProductImageEntity;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {
    List<ProductImageEntity> findByProductId(Long productId);

    void deleteByProductId(Long productId);
}
