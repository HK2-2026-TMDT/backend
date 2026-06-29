package vn.io.sanmaymac.modules.user.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.user.entity.WorkshopFavoriteEntity;

public interface WorkshopFavoriteRepository extends JpaRepository<WorkshopFavoriteEntity, Long> {
    List<WorkshopFavoriteEntity> findByCustomerId(Long customerId);

    Optional<WorkshopFavoriteEntity> findByCustomerIdAndWorkshopId(Long customerId, Long workshopId);
}
