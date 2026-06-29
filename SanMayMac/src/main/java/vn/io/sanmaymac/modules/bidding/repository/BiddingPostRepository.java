package vn.io.sanmaymac.modules.bidding.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.common.enums.PostStatus;
import vn.io.sanmaymac.modules.bidding.entity.BiddingPostEntity;

public interface BiddingPostRepository extends JpaRepository<BiddingPostEntity, Long> {
    List<BiddingPostEntity> findByCustomerId(Long customerId);

    List<BiddingPostEntity> findByStatus(PostStatus status);

    Page<BiddingPostEntity> findByCustomerId(Long customerId, Pageable pageable);

    Page<BiddingPostEntity> findByStatus(PostStatus status, Pageable pageable);
}
