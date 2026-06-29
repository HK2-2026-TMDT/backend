package vn.io.sanmaymac.modules.user.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.user.entity.WorkshopKycDocumentEntity;

public interface WorkshopKycDocumentRepository extends JpaRepository<WorkshopKycDocumentEntity, Long> {
    Optional<WorkshopKycDocumentEntity> findByUserId(Long userId);
}
