package vn.io.sanmaymac.modules.user.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.io.sanmaymac.modules.user.entity.WorkshopProfileEntity;

public interface WorkshopProfileRepository extends JpaRepository<WorkshopProfileEntity, Long> {
    Optional<WorkshopProfileEntity> findByUserId(Long userId);

    @Query("""
            SELECT p FROM WorkshopProfileEntity p
            JOIN p.user u
            WHERE u.status = 'ACTIVE'
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(p.shopName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:verifiedOnly = false OR p.isVerified = true)
            ORDER BY p.ratingAvg DESC NULLS LAST
            """)
    Page<WorkshopProfileEntity> findPublicWorkshops(
            @Param("keyword") String keyword,
            @Param("verifiedOnly") boolean verifiedOnly,
            Pageable pageable);
}
