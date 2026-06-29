package vn.io.sanmaymac.modules.user.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.user.entity.WorkshopPortfolioEntity;

public interface WorkshopPortfolioRepository extends JpaRepository<WorkshopPortfolioEntity, Long> {
    List<WorkshopPortfolioEntity> findByWorkshopId(Long workshopId);

    Optional<WorkshopPortfolioEntity> findByIdAndWorkshopId(Long id, Long workshopId);
}
