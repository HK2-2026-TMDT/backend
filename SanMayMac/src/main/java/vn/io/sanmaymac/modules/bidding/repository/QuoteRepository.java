package vn.io.sanmaymac.modules.bidding.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.common.enums.QuoteStatus;
import vn.io.sanmaymac.modules.bidding.entity.QuoteEntity;

public interface QuoteRepository extends JpaRepository<QuoteEntity, Long> {
	List<QuoteEntity> findByPostId(Long postId);

	Optional<QuoteEntity> findByPostIdAndWorkshopId(Long postId, Long workshopId);

	List<QuoteEntity> findByWorkshopId(Long workshopId);

	List<QuoteEntity> findByStatus(QuoteStatus status);

	Page<QuoteEntity> findByPostId(Long postId, Pageable pageable);

	Page<QuoteEntity> findByWorkshopId(Long workshopId, Pageable pageable);

	Page<QuoteEntity> findByStatus(QuoteStatus status, Pageable pageable);

	long countByPostId(Long postId);

	void deleteByPostId(Long postId);
}
