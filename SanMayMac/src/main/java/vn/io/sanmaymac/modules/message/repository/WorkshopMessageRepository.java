package vn.io.sanmaymac.modules.message.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.message.entity.WorkshopMessageEntity;

public interface WorkshopMessageRepository extends JpaRepository<WorkshopMessageEntity, Long> {
    List<WorkshopMessageEntity> findByThreadIdOrderByCreatedAtAsc(Long threadId);
}