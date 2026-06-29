package vn.io.sanmaymac.modules.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.finance.entity.CommissionConfigEntity;

public interface CommissionConfigRepository extends JpaRepository<CommissionConfigEntity, Long> {
}
