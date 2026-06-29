package vn.io.sanmaymac.modules.finance.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.common.enums.PayoutStatus;
import vn.io.sanmaymac.modules.finance.entity.PayoutRequestEntity;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequestEntity, Long> {
    List<PayoutRequestEntity> findByWorkshopId(Long workshopId);

    List<PayoutRequestEntity> findByStatus(PayoutStatus status);

    java.util.Optional<PayoutRequestEntity> findByIdAndWorkshopId(Long id, Long workshopId);
}
