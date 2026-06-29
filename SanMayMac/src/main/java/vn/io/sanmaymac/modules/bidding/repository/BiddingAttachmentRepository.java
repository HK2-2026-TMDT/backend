package vn.io.sanmaymac.modules.bidding.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.bidding.entity.BiddingAttachmentEntity;

public interface BiddingAttachmentRepository extends JpaRepository<BiddingAttachmentEntity, Long> {
    List<BiddingAttachmentEntity> findByPostId(Long postId);

    void deleteByPostId(Long postId);
}
