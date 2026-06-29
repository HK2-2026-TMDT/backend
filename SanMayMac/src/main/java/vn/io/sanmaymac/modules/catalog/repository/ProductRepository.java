package vn.io.sanmaymac.modules.catalog.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.common.enums.ProductApprovalStatus;
import vn.io.sanmaymac.modules.catalog.entity.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
	Page<ProductEntity> findByWorkshopId(Long workshopId, Pageable pageable);

	Page<ProductEntity> findByApprovalStatus(ProductApprovalStatus approvalStatus, Pageable pageable);

	List<ProductEntity> findTop20ByIsVisibleTrueAndApprovalStatusOrderByCreatedAtDesc(ProductApprovalStatus approvalStatus);
}
