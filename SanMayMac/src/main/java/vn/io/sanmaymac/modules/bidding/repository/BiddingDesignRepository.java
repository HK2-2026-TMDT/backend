package vn.io.sanmaymac.modules.bidding.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.bidding.entity.BiddingDesignEntity;

public interface BiddingDesignRepository extends JpaRepository<BiddingDesignEntity, Long> {
	List<BiddingDesignEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
